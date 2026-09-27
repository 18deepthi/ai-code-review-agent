import React, { useState, useEffect } from 'react';
import { History, TrendingUp, CheckCircle2, Clock, Code, ChevronDown, ChevronUp, Brain, AlertCircle } from 'lucide-react';
import { fetchTeamSubmissions } from '../services/api';

export default function HistoryPage({ teamId }) {
  const [submissions, setSubmissions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [expandedId, setExpandedId] = useState(null);

  const loadHistory = async () => {
    setLoading(true);
    try {
      const data = await fetchTeamSubmissions(teamId);
      setSubmissions(data || []);
      if (data && data.length > 0) {
        setExpandedId(data[0].id);
      }
    } catch (err) {
      console.error('Failed to load history:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadHistory();
  }, [teamId]);

  const toggleExpand = (id) => {
    setExpandedId(expandedId === id ? null : id);
  };

  const totalReviews = submissions.length;
  const totalIssuesReviewed = submissions.reduce((sum, s) => sum + (s.totalIssuesCount || 0), 0);
  const totalSkippedByMemory = submissions.reduce((sum, s) => sum + (s.skippedCount || 0), 0);

  return (
    <div>
      <div className="page-header" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 className="page-title">
            <History color="#6366f1" size={32} />
            Review History & Learning Evolution
          </h1>
          <p className="page-subtitle">
            Observe how review outcomes change over time for team <strong style={{ color: '#c084fc' }}>{teamId}</strong> as 
            the agent remembers team feedback and proactively skips overridden preferences.
          </p>
        </div>
      </div>

      {/* Evolution Summary Cards */}
      <div className="stats-row">
        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'rgba(99, 102, 241, 0.2)', color: '#818cf8' }}>
            <History size={24} />
          </div>
          <div>
            <div className="stat-value">{totalReviews}</div>
            <div className="stat-label">Total Submissions Reviewed</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'rgba(244, 63, 94, 0.2)', color: '#fda4af' }}>
            <AlertCircle size={24} />
          </div>
          <div>
            <div className="stat-value">{totalIssuesReviewed}</div>
            <div className="stat-label">Total Issues Analyzed</div>
          </div>
        </div>

        <div className="stat-card" style={{ borderColor: 'rgba(168, 85, 247, 0.4)', boxShadow: 'var(--glow-memory)' }}>
          <div className="stat-icon" style={{ background: 'rgba(168, 85, 247, 0.2)', color: '#c084fc' }}>
            <TrendingUp size={24} />
          </div>
          <div>
            <div className="stat-value" style={{ color: '#e9d5ff' }}>{totalSkippedByMemory}</div>
            <div className="stat-label">Total Skipped Due to Learned Memory</div>
          </div>
        </div>
      </div>

      {submissions.length === 0 && !loading && (
        <div className="glass-card" style={{ textAlign: 'center', padding: '4rem 2rem', color: 'var(--text-muted)' }}>
          <History size={48} style={{ opacity: 0.2, margin: '0 auto 1rem' }} />
          <p style={{ fontWeight: 600, color: 'var(--text-main)' }}>No review submissions on record for team "{teamId}"</p>
          <p style={{ fontSize: '0.875rem', marginTop: '0.25rem' }}>
            Submit code on the "Submit & Review" tab to begin tracking review history.
          </p>
        </div>
      )}

      {/* Submissions Timeline List */}
      <div>
        {submissions.map((sub, index) => {
          const isExpanded = expandedId === sub.id;
          const runNumber = submissions.length - index;

          return (
            <div key={sub.id} className="history-item">
              <div 
                className="history-header" 
                onClick={() => toggleExpand(sub.id)}
                style={{ cursor: 'pointer', userSelect: 'none' }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                  <span style={{ 
                    background: 'rgba(255,255,255,0.08)', 
                    padding: '0.25rem 0.6rem', 
                    borderRadius: '6px', 
                    fontWeight: 700, 
                    fontSize: '0.85rem' 
                  }}>
                    Review #{runNumber}
                  </span>
                  <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                    <Clock size={14} /> {sub.timestamp ? new Date(sub.timestamp).toLocaleString() : 'Recent'}
                  </span>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                  <span className="badge" style={{ background: 'rgba(99, 102, 241, 0.15)', color: '#a5b4fc' }}>
                    Issues: {sub.totalIssuesCount}
                  </span>
                  {sub.skippedCount > 0 ? (
                    <span className="badge badge-skipped-memory">
                      <Brain size={12} /> {sub.skippedCount} Skipped by Memory
                    </span>
                  ) : (
                    <span className="badge" style={{ background: 'rgba(255,255,255,0.06)', color: 'var(--text-muted)' }}>
                      0 Skipped (Generic baseline)
                    </span>
                  )}
                  {isExpanded ? <ChevronUp size={18} /> : <ChevronDown size={18} />}
                </div>
              </div>

              {/* Recalled context summary */}
              {sub.recalledMemories && sub.recalledMemories.length > 0 && (
                <div style={{ fontSize: '0.8rem', color: '#c084fc', marginBottom: '0.5rem', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                  <Brain size={14} /> Recalled {sub.recalledMemories.length} team preferences from Hindsight
                </div>
              )}

              {/* Expanded details */}
              {isExpanded && (
                <div style={{ marginTop: '1rem', borderTop: '1px solid var(--border-subtle)', paddingTop: '1rem' }}>
                  <div style={{ marginBottom: '1rem' }}>
                    <div style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.4rem', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                      <Code size={14} /> Reviewed Code Snippet:
                    </div>
                    <pre style={{ 
                      background: '#0d1117', 
                      padding: '0.85rem', 
                      borderRadius: '6px', 
                      fontSize: '0.8rem', 
                      overflowX: 'auto', 
                      fontFamily: 'var(--font-mono)',
                      color: '#e6edf3',
                      maxHeight: '180px'
                    }}>
                      {sub.codeSnippet}
                    </pre>
                  </div>

                  <div>
                    <div style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.4rem' }}>
                      Review Outcome Comments:
                    </div>
                    {sub.comments?.map((c) => (
                      <div 
                        key={c.id} 
                        style={{ 
                          background: c.skippedDueToMemory ? 'rgba(88, 28, 135, 0.2)' : 'rgba(0,0,0,0.25)', 
                          border: c.skippedDueToMemory ? '1px solid rgba(168, 85, 247, 0.4)' : '1px solid var(--border-subtle)',
                          borderRadius: '6px',
                          padding: '0.75rem',
                          marginBottom: '0.5rem'
                        }}
                      >
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.25rem' }}>
                          <span style={{ fontWeight: 600, fontSize: '0.9rem' }}>{c.issue}</span>
                          {c.skippedDueToMemory ? (
                            <span className="badge badge-skipped-memory">
                              {c.memoryReason || 'Skipped via Memory'}
                            </span>
                          ) : (
                            <span className={`badge badge-${c.severity || 'medium'}`}>
                              {c.severity || 'Medium'}
                            </span>
                          )}
                        </div>
                        {c.suggestion && (
                          <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)', marginTop: '0.25rem' }}>
                            {c.suggestion}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
