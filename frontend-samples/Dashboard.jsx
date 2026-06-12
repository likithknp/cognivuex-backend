import React, { useEffect, useState } from 'react';
import axios from 'axios';

// Simple Dashboard sample component that reads the most-recent uploaded report
// and prediction from localStorage (set by Upload.jsx). If not present it will
// fetch the latest report from the backend GET /api/reports.
// Drop this into your React app (e.g. src/components/Dashboard.jsx) and adapt
// styling to your project.

export default function Dashboard() {
  const [report, setReport] = useState(null);
  const [prediction, setPrediction] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    // Prefer the last uploaded report stored by Upload.jsx for immediate UX
    try {
      const stored = localStorage.getItem('lastUploadedReport');
      const storedResp = localStorage.getItem('uploadResponse');
      if (storedResp) {
        const parsed = JSON.parse(storedResp);
        if (parsed?.report) {
          setReport(parsed.report);
        }
        if (parsed?.prediction) {
          setPrediction(parsed.prediction);
        }
        setLoading(false);
        return;
      }
      if (stored) {
        setReport(JSON.parse(stored));
        setLoading(false);
        return;
      }
    } catch (e) {
      // ignore parse errors and fallback to fetch
    }

    // Fallback: fetch latest reports from backend
    (async () => {
      try {
        const res = await axios.get('https://cognivuex-backend-1.onrender.com/api/reports');
        // assuming backend returns list and newest comes first (adjust if needed)
        if (Array.isArray(res.data) && res.data.length > 0) {
          setReport(res.data[0]);
        } else {
          setReport(null);
        }
      } catch (err) {
        setError(err.message || 'Failed to fetch reports');
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  if (loading) return <div style={{padding:20}}>Loading dashboard...</div>;
  if (error) return <div style={{padding:20,color:'red'}}>Error: {error}</div>;

  // Helper to show numeric value or a friendly placeholder
  function showNumber(v) {
    if (v === null || v === undefined) return '--';
    return v;
  }

  // Use prediction values first if available for UI widgets, else use report fields
  const riskScore = prediction?.riskScore ?? report?.riskScore ?? 0;
  const riskLevel = prediction?.riskLevel ?? (report?.riskLevel ?? 'Unknown');
  const wellness = report?.wellnessScore ?? Math.max(0, 100 - (riskScore || 0));
  const recommendation = (prediction?.aiRecommendation ?? report?.suggestions) || 'No recommendations available';

  return (
    <div style={{padding:24,fontFamily:'Segoe UI, Roboto, Arial'}}>
      <header style={{display:'flex',justifyContent:'space-between',alignItems:'center'}}>
        <h1>CognivueX — Health Intelligence Dashboard</h1>
        <div style={{textAlign:'right'}}>
          <div style={{fontSize:12,color:'#666'}}>Patient</div>
          <div style={{fontWeight:700}}>{report?.patientName ?? 'Unknown'}</div>
          <div style={{fontSize:12,color:'#666'}}>Age: {showNumber(report?.age)}</div>
        </div>
      </header>

      <section style={{display:'flex',gap:20,marginTop:20}}>
        <div style={{flex:'0 0 260px',padding:20,background:'#f5f7fb',borderRadius:8}}>
          <div style={{fontSize:12,color:'#444'}}>AI Wellness Score</div>
          <div style={{fontSize:48,fontWeight:700,color:'#0ea5a4'}}>{showNumber(wellness)}</div>
          <div style={{marginTop:12}}>Risk Level: <strong>{riskLevel}</strong></div>
          <div style={{marginTop:8}}>Risk Score: <strong>{showNumber(riskScore)}</strong></div>
        </div>

        <div style={{flex:1,padding:20,background:'#fff',borderRadius:8,boxShadow:'0 1px 4px rgba(0,0,0,0.06)'}}>
          <h2>Key Vitals</h2>
          <div style={{display:'grid',gridTemplateColumns:'repeat(3,1fr)',gap:12}}>
            <div style={{padding:12,background:'#fafafa',borderRadius:6}}>
              <div style={{fontSize:12,color:'#666'}}>Glucose</div>
              <div style={{fontWeight:700}}>{showNumber(report?.glucose)} mg/dL</div>
            </div>
            <div style={{padding:12,background:'#fafafa',borderRadius:6}}>
              <div style={{fontSize:12,color:'#666'}}>HbA1c</div>
              <div style={{fontWeight:700}}>{showNumber(report?.hba1c)} %</div>
            </div>
            <div style={{padding:12,background:'#fafafa',borderRadius:6}}>
              <div style={{fontSize:12,color:'#666'}}>BMI</div>
              <div style={{fontWeight:700}}>{showNumber(report?.bmi)}</div>
            </div>

            <div style={{padding:12,background:'#fafafa',borderRadius:6}}>
              <div style={{fontSize:12,color:'#666'}}>Systolic BP</div>
              <div style={{fontWeight:700}}>{showNumber(report?.systolicBP)} mmHg</div>
            </div>
            <div style={{padding:12,background:'#fafafa',borderRadius:6}}>
              <div style={{fontSize:12,color:'#666'}}>Diastolic BP</div>
              <div style={{fontWeight:700}}>{showNumber(report?.diastolicBP)} mmHg</div>
            </div>
            <div style={{padding:12,background:'#fafafa',borderRadius:6}}>
              <div style={{fontSize:12,color:'#666'}}>Cholesterol</div>
              <div style={{fontWeight:700}}>{showNumber(report?.cholesterol)} mg/dL</div>
            </div>
          </div>

          <h3 style={{marginTop:18}}>AI Health Recommendations</h3>
          <div style={{padding:12,background:'#fff6f5',borderRadius:6,border:'1px solid #ffd7d0'}}>
            {recommendation}
          </div>
        </div>
      </section>

      <section style={{marginTop:20}}>
        <h3>Raw saved report (for debugging)</h3>
        <pre style={{background:'#0f172a',color:'#e6eef8',padding:12,borderRadius:6,overflowX:'auto'}}>
          {JSON.stringify(report, null, 2)}
        </pre>
      </section>
    </div>
  );
}

