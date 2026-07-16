require('dotenv').config();
const express = require('express');
const cors = require('cors');
const multer = require('multer');
const fs = require('fs');
const path = require('path');

let pdfParse, mammoth, fetch;
try { pdfParse = require('pdf-parse'); } catch (e) { console.warn('pdf-parse not installed, PDF parsing disabled'); }
try { mammoth = require('mammoth'); } catch (e) { console.warn('mammoth not installed, DOCX parsing disabled'); }
try { fetch = require('node-fetch'); } catch (e) { fetch = global.fetch; }

const upload = multer({ storage: multer.memoryStorage() });
const app = express();
app.use(cors());

const UPLOAD_DIR = path.join(__dirname, 'uploads');
const REPORTS_FILE = path.join(__dirname, 'reports.json');
if (!fs.existsSync(UPLOAD_DIR)) fs.mkdirSync(UPLOAD_DIR, { recursive: true });
if (!fs.existsSync(REPORTS_FILE)) fs.writeFileSync(REPORTS_FILE, JSON.stringify([]));

function clamp(v, a = 0, b = 100) { return Math.max(a, Math.min(b, v)); }

async function extractTextFromFile(file) {
  const name = (file.originalname || '').toLowerCase();
  const textFromBuffer = file.buffer ? file.buffer.toString('utf8') : '';

  if (name.endsWith('.pdf') && pdfParse) {
    try { const data = await pdfParse(file.buffer); return (data.text || textFromBuffer).toLowerCase(); } catch (e) { return textFromBuffer.toLowerCase(); }
  }

  if ((name.endsWith('.docx') || name.endsWith('.doc')) && mammoth) {
    try { const result = await mammoth.extractRawText({ buffer: file.buffer }); return (result.value || textFromBuffer).toLowerCase(); } catch (e) { return textFromBuffer.toLowerCase(); }
  }

  return textFromBuffer.toLowerCase();
}

async function callOpenAI(prompt, apiKey) {
  if (!apiKey) return null;
  try {
    const body = {
      model: 'gpt-3.5-turbo',
      messages: [
        { role: 'system', content: 'You are a helpful medical assistant. Return a JSON object with keys: answer (string), score (0-100 integer), findings (array of short strings).' },
        { role: 'user', content: prompt },
      ],
      temperature: 0.2,
      max_tokens: 800,
    };

    const r = await fetch('https://api.openai.com/v1/chat/completions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${apiKey}`,
      },
      body: JSON.stringify(body),
    });

    if (!r.ok) { console.warn('OpenAI call failed', r.status); return null; }

    const j = await r.json();
    const text = j?.choices?.[0]?.message?.content || null;
    if (!text) return null;

    const jsonMatch = text.match(/\{[\s\S]*\}/);
    if (jsonMatch) { try { return JSON.parse(jsonMatch[0]); } catch (e) { return { answer: text }; } }
    return { answer: text };
  } catch (e) { console.error('OpenAI request error', e.message); return null; }
}

app.post('/api/copilot', upload.array('files'), async (req, res) => {
  const question = req.body.question || '';
  const patientId = req.body.patientId || null;
  const files = req.files || [];

  const findings = [];
  let score = 80;
  let combinedText = question.toLowerCase();
  const savedReports = [];

  for (const file of files) {
    try {
      const safeName = `${Date.now()}_${file.originalname.replace(/[^a-z0-9_.-]/gi, '_')}`;
      const outPath = path.join(UPLOAD_DIR, safeName);
      fs.writeFileSync(outPath, file.buffer);
      savedReports.push({ originalName: file.originalname, path: outPath, size: file.size, mimetype: file.mimetype });

      const text = await extractTextFromFile(file);
      combinedText += '\n' + text;

      // DEBUG: log filename and extracted snippet
      console.log('Received file:', file.originalname, 'size:', file.size, 'mimetype:', file.mimetype);
      console.log('Extracted text (first 400 chars):', (text || '').slice(0, 400).replace(/\n/g, ' '));

      const name = (file.originalname || '').toLowerCase();

      // richer numeric parsing and heuristics
      function analyzeMetrics(text) {
        const localFindings = [];
        let delta = 0;

        // Total cholesterol
        const totalChol = (text.match(/(?:total\s+)?cholesterol[:\s]*([0-9]{2,3})/) || [])[1];
        if (totalChol) {
          const val = parseInt(totalChol, 10);
          localFindings.push(`Total cholesterol ${val} mg/dL`);
          if (val > 240) delta -= 22;
          else if (val > 200) delta -= 12;
        }

        // LDL
        const ldl = (text.match(/ldl[:\s]*([0-9]{2,3})/) || [])[1];
        if (ldl) {
          const v = parseInt(ldl, 10);
          localFindings.push(`LDL ${v} mg/dL`);
          if (v > 160) delta -= 18;
          else if (v > 130) delta -= 10;
        }

        // HDL
        const hdl = (text.match(/hdl[:\s]*([0-9]{2,3})/) || [])[1];
        if (hdl) {
          const v = parseInt(hdl, 10);
          localFindings.push(`HDL ${v} mg/dL`);
          if (v < 40) delta -= 8;
        }

        // Triglycerides
        const tg = (text.match(/triglycerides[:\s]*([0-9]{2,4})/) || [])[1];
        if (tg) {
          const v = parseInt(tg, 10);
          localFindings.push(`Triglycerides ${v} mg/dL`);
          if (v > 500) delta -= 18;
          else if (v > 200) delta -= 10;
          else if (v > 150) delta -= 6;
        }

        // HbA1c
        const hba1c = (text.match(/hba1c[:\s]*([0-9]{1,2}\.?[0-9]?)/) || [])[1] || (text.match(/a1c[:\s]*([0-9]{1,2}\.?[0-9]?)/) || [])[1];
        if (hba1c) {
          const v = parseFloat(hba1c);
          localFindings.push(`HbA1c ${v}%`);
          if (v >= 6.5) delta -= 20;
          else if (v >= 5.7) delta -= 10;
        }

        // Glucose (fasting)
        const glucose = (text.match(/(?:fasting\s+)?glucose[:\s]*([0-9]{2,3})/) || [])[1];
        if (glucose) {
          const v = parseInt(glucose, 10);
          localFindings.push(`Glucose ${v} mg/dL`);
          if (v >= 126) delta -= 20;
          else if (v >= 100) delta -= 8;
        }

        // Blood pressure
        const bpMatch = text.match(/(\d{2,3})\/(\d{2,3})/);
        if (bpMatch) {
          const sys = parseInt(bpMatch[1], 10);
          const dia = parseInt(bpMatch[2], 10);
          localFindings.push(`BP ${sys}/${dia} mmHg`);
          if (sys >= 180 || dia >= 120) delta -= 30;
          else if (sys >= 140 || dia >= 90) delta -= 12;
        }

        // generic keywords
        if (text.match(/hypertension/)) { localFindings.push('Hypertension'); delta -= 10; }
        if (text.match(/high cholesterol|hyperlipidemia/)) { localFindings.push('High cholesterol note'); delta -= 10; }

        return { localFindings, delta };
      }

      const analysis = analyzeMetrics(text);
      if (analysis.localFindings.length) {
        findings.push(...analysis.localFindings);
      }
      // apply delta
      score += analysis.delta || 0;

      // fallback keyword checks for older behavior
      if (name.includes('cholesterol') || text.includes('cholesterol')) { /* handled above */ }
      if (text.match(/blood pressure|bp|hypertension|systolic/)) { /* handled above */ }
      if (text.match(/glucose|hba1c|sugar/)) { /* handled above */ }


    } catch (e) { console.error('file processing error', e.message); }
  }

  const q = (question || '').toLowerCase();
  if (q.includes('sleep')) { findings.push('Sleep pattern requested'); }
  if (q.includes('diet') || q.includes('nutrition')) { findings.push('Nutrition requested'); }

  score = clamp(score, 5, 99);

  let finalFindings = findings.slice();
  let finalScore = score;

  function generateHeuristicAnswer(questionText, combinedText, findingsList, scoreVal) {
    let summary = `Summary of analysis based on ${files.length} uploaded file(s):`;
    if (findingsList.length) {
      summary += ' ' + findingsList.join('; ') + '.';
    } else {
      summary += ' No critical flags detected in the parsed report text.';
    }

    let advice = '\n\nRecommendations:\n';
    if (findingsList.some(f => /cholesterol/i.test(f))) {
      advice += '- High cholesterol detected. Recommend dietary changes (reduce saturated fat), consider statin evaluation, and recheck lipid panel in 3 months.\n';
    }
    if (findingsList.some(f => /bp|blood pressure|hypertension/i.test(f))) {
      advice += '- Blood pressure appears elevated. Measure home BP daily, reduce sodium, increase aerobic activity, and consult physician for medication review.\n';
    }
    if (findingsList.some(f => /glucose|hba1c|sugar/i.test(f))) {
      advice += '- Elevated glucose levels noted. Review fasting glucose/HbA1c; advise dietary carbohydrate control, weight management, and consider endocrine consult.\n';
    }
    if (!advice.trim()) advice += '- Maintain healthy diet, regular activity, and follow up with your clinician for personalized care.\n';

    const heuristic = `${summary}\nEstimated health score (heuristic): ${scoreVal}%${advice}`;
    if (questionText) return `${heuristic}\n\nUser question: ${questionText}`;
    return heuristic;
  }

  let finalAnswer = generateHeuristicAnswer(question, combinedText, finalFindings, finalScore);

  const OPENAI_KEY = process.env.OPENAI_API_KEY || null;
  if (OPENAI_KEY) {
    const prompt = `Question: ${question}\n\nContext (extracted from uploaded reports):\n${combinedText}\n\nReturn a JSON object with keys: answer (string), score (int 0-100), findings (array of short strings).`;
    const aiResult = await callOpenAI(prompt, OPENAI_KEY);
    if (aiResult) {
      if (typeof aiResult.score === 'number') finalScore = clamp(aiResult.score, 0, 100);
      if (Array.isArray(aiResult.findings) && aiResult.findings.length) finalFindings = aiResult.findings;
      if (aiResult.answer) finalAnswer = aiResult.answer;
    } else {
      // keep heuristic answer if AI failed
      finalAnswer = generateHeuristicAnswer(question, combinedText, finalFindings, finalScore);
    }
  }

  try {
    const reports = JSON.parse(fs.readFileSync(REPORTS_FILE, 'utf8') || '[]');
    const record = { id: Date.now(), patientId, question, timestamp: new Date().toISOString(), files: savedReports, score: finalScore, findings: finalFindings };
    reports.push(record);
    fs.writeFileSync(REPORTS_FILE, JSON.stringify(reports, null, 2));
  } catch (e) { console.error('reports persistence error', e.message); }

  return res.json({ answer: finalAnswer, score: finalScore, findings: finalFindings, extractedText: combinedText });
});

// return latest saved report
app.get('/api/reports/latest', (req, res) => {
  try {
    const reports = JSON.parse(fs.readFileSync(REPORTS_FILE, 'utf8') || '[]');
    const last = reports.length ? reports[reports.length - 1] : null;
    return res.json({ latest: last });
  } catch (e) {
    console.error('reports read error', e.message);
    return res.status(500).json({ error: 'could not read reports' });
  }
});

const port = process.env.PORT || process.env.PORT || 4000;
app.listen(port, () => console.log(`Copilot analysis server listening on http://localhost:${port}`));
