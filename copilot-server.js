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
      if (name.includes('cholesterol') || text.includes('cholesterol')) { findings.push('High cholesterol'); score -= 15; }
      if (text.match(/blood pressure|bp|hypertension|systolic/)) { findings.push('Blood pressure concerns'); score -= 12; }
      if (text.match(/glucose|hba1c|sugar/)) { findings.push('Elevated glucose'); score -= 12; }

      const cholMatch = text.match(/cholesterol[:\s]*([0-9]{2,3})/);
      if (cholMatch) { const val = parseInt(cholMatch[1], 10); if (val > 200) { findings.push(`Cholesterol ${val} mg/dL`); score -= Math.min(20, Math.floor((val - 200) / 2)); } }

      const bpMatch = text.match(/(\d{2,3})\/(\d{2,3})/);
      if (bpMatch) { const sys = parseInt(bpMatch[1], 10); const dia = parseInt(bpMatch[2], 10); if (sys >= 140 || dia >= 90) { findings.push(`BP ${sys}/${dia} mmHg`); score -= 12; } }

    } catch (e) { console.error('file processing error', e.message); }
  }

  const q = (question || '').toLowerCase();
  if (q.includes('sleep')) { findings.push('Sleep pattern requested'); }
  if (q.includes('diet') || q.includes('nutrition')) { findings.push('Nutrition requested'); }

  score = clamp(score, 5, 99);

  let finalAnswer = `Based on ${files.length} uploaded file(s) and your question, here is a concise analysis and recommended next steps.`;
  let finalFindings = findings.slice();
  let finalScore = score;

  const OPENAI_KEY = process.env.OPENAI_API_KEY || null;
  if (OPENAI_KEY) {
    const prompt = `Question: ${question}\n\nContext (extracted from uploaded reports):\n${combinedText}\n\nReturn a JSON object with keys: answer (string), score (int 0-100), findings (array of short strings).`;
    const aiResult = await callOpenAI(prompt, OPENAI_KEY);
    if (aiResult) {
      if (typeof aiResult.score === 'number') finalScore = clamp(aiResult.score, 0, 100);
      if (Array.isArray(aiResult.findings) && aiResult.findings.length) finalFindings = aiResult.findings;
      if (aiResult.answer) finalAnswer = aiResult.answer;
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

const port = process.env.PORT || process.env.PORT || 4000;
app.listen(port, () => console.log(`Copilot analysis server listening on http://localhost:${port}`));
