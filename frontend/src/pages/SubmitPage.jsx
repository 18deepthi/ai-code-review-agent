import React, { useState } from 'react';
import { Play, Sparkles, Check, X, ShieldAlert, ArrowRight, Brain, RotateCcw, FileCode, CheckCircle2, Users } from 'lucide-react';
import { SYNTHETIC_SNIPPETS } from '../data/syntheticSnippets';
import { submitCodeReview, sendFeedback } from '../services/api';

export default function SubmitPage({ teamId, setTeamId, onFeedbackGiven }) {
  const [selectedSnippetId, setSelectedSnippetId] = useState(SYNTHETIC_SNIPPETS[0].id);
  const [code, setCode] = useState(SYNTHETIC_SNIPPETS[0].code);
  const [loading, setLoading] = useState(false);
  const [reviewResult, setReviewResult] = useState(null);
  const [notesByComment, setNotesByComment] = useState({});
  const [feedbackStatus, setFeedbackStatus] = useState({});
  const [error, setError] = useState(null);

  const handleSelectSnippet = (id) => {
    setSelectedSnippetId(id);
    const snippet = SYNTHETIC_SNIPPETS.find((s) => s.id === id);
    if (snippet) {
      setCode(snippet.code);
      setReviewResult(null);
      setError(null);
    }
  };

  // Reset state when active team changes to ensure zero cross-team UI pollution
  React.useEffect(() => {
    setReviewResult(null);
    setFeedbackStatus({});
    setNotesByComment({});
    setError(null);
  }, [teamId]);

  const handleRunReview = async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await submitCodeReview(teamId, code, 'java');
      setReviewResult(result);
      setFeedbackStatus({});
    } catch (err) {
      setError(err.message || 'Review failed to execute');
    } finally {
      setLoading(false);
    }
  };

  const handleFeedback = async (comment, decision) => {
    const commentId = comment.id;
    const note = notesByComment[commentId] || '';
    const selectedSnippet = SYNTHETIC_SNIPPETS.find((s) => s.id === selectedSnippetId);
    const ruleText = decision === 'rejected'
      ? (note || (selectedSnippet?.exampleOverrideNote || comment.issue))
      : comment.issue;

    try {
      await sendFeedback({
        teamId,
        reviewId: reviewResult?.reviewId || 'rev-current',
        commentId,
        decision,
        note,
        rule: ruleText,
        ruleKey: comment.ruleKey || '',
        sourceSnippetExcerpt: code.slice(0, 150)
      });

      setFeedbackStatus((prev) => ({
        ...prev,
        [commentId]: {
          decision,
          message: decision === 'rejected' 
            ? 'Saved to Hindsight! Next review will skip this.' 
            : 'Enforced guideline recorded.'
        }
      }));

      if (onFeedbackGiven) {
        onFeedbackGiven();
      }
    } catch (err) {
      alert('Failed to save feedback: ' + err.message);
    }
  };

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">
          <Brain color="#a855f7" size={32} />
          Java Code Review with Persistent Memory
        </h1>
        <p className="page-subtitle">
          Submit code for review. The agent checks Hindsight for your team's historical feedback, 
          silently suppresses previously rejected recommendations, and gets smarter with every interaction.
        </p>
      </div>

      {/* Team Selector Bar */}
      <div className="snippet-selector-bar" style={{ borderColor: 'var(--border-memory)', marginBottom: '0.75rem', background: 'rgba(168, 85, 247, 0.08)' }}>
        <span style={{ fontSize: '0.85rem', fontWeight: 600, color: '#e9d5ff', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
          <Users size={16} color="#c084fc" /> Active Team Memory Bank:
        </span>
        <input
          id="submit-team-id-input"
          type="text"
          className="team-input"
          style={{ background: 'rgba(0,0,0,0.3)', border: '1px solid var(--border-subtle)', borderRadius: '4px', padding: '0.35rem 0.6rem', color: '#fff', width: '200px' }}
          value={teamId}
          onChange={(e) => setTeamId(e.target.value)}
          placeholder="Enter team ID"
        />
        <div style={{ display: 'flex', gap: '0.35rem', alignItems: 'center', flexWrap: 'wrap' }}>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Quick Select:</span>
          <button
            type="button"
            className="btn-secondary"
            style={{ padding: '0.25rem 0.6rem', fontSize: '0.75rem', background: teamId === 'team-payments-legacy' ? 'rgba(168, 85, 247, 0.25)' : '' }}
            onClick={() => setTeamId('team-payments-legacy')}
          >
            team-payments-legacy
          </button>
          <button
            type="button"
            className="btn-secondary"
            style={{ padding: '0.25rem 0.6rem', fontSize: '0.75rem', background: teamId === 'team-backend-core' ? 'rgba(168, 85, 247, 0.25)' : '' }}
            onClick={() => setTeamId('team-backend-core')}
          >
            team-backend-core
          </button>
          <button
            type="button"
            className="btn-secondary"
            style={{ padding: '0.25rem 0.6rem', fontSize: '0.75rem', background: teamId === 'team-alpha' ? 'rgba(168, 85, 247, 0.25)' : '' }}
            onClick={() => setTeamId('team-alpha')}
          >
            team-alpha
          </button>
        </div>
      </div>

      {/* Preset Snippet Selector */}
      <div className="snippet-selector-bar">
        <span style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
          <FileCode size={16} color="#6366f1" /> Load Synthetic Scenario:
        </span>
        <select
          id="snippet-dropdown"
          className="snippet-select-dropdown"
          value={selectedSnippetId}
          onChange={(e) => handleSelectSnippet(e.target.value)}
        >
          {SYNTHETIC_SNIPPETS.map((snippet) => (
            <option key={snippet.id} value={snippet.id}>
              [{snippet.categoryLabel}] {snippet.title}
            </option>
          ))}
        </select>
        <button
          id="btn-reset-code"
          className="btn-secondary"
          onClick={() => {
            const snip = SYNTHETIC_SNIPPETS.find((s) => s.id === selectedSnippetId);
            if (snip) setCode(snip.code);
          }}
          title="Reset code to original snippet template"
        >
          <RotateCcw size={14} /> Reset
        </button>
      </div>

      <div className="grid-2col">
        {/* Left Column: Code Editor */}
        <div>
          <div className="code-editor-wrapper">
            <div className="editor-header">
              <span>Java 17 &bull; Spring Boot Source</span>
              <span>Memory Bank: <strong style={{ color: '#c084fc' }}>{teamId}</strong></span>
            </div>
            <textarea
              id="code-editor-input"
              className="code-textarea"
              value={code}
              onChange={(e) => setCode(e.target.value)}
              placeholder="Paste or write Java code here..."
              spellCheck="false"
            />
          </div>

          <div style={{ marginTop: '1rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <button
              id="btn-run-review"
              className="btn-primary"
              onClick={handleRunReview}
              disabled={loading || !code.trim()}
            >
              <Play size={16} fill="white" />
              {loading ? 'Consulting Hindsight & Reviewing...' : 'Review Code with Memory'}
            </button>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
              Recall &bull; Reason &bull; Skip known preferences
            </span>
          </div>

          {error && (
            <div style={{ marginTop: '1rem', padding: '0.75rem 1rem', background: 'rgba(244, 63, 94, 0.15)', border: '1px solid rgba(244, 63, 94, 0.3)', borderRadius: '8px', color: '#fda4af', fontSize: '0.875rem' }}>
              {error}
            </div>
          )}
        </div>

        {/* Right Column: Review Results */}
        <div>
          <div className="glass-card" style={{ minHeight: '520px' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '0.75rem' }}>
              <h3 style={{ fontSize: '1.1rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Sparkles size={18} color="#a855f7" /> Review Output
              </h3>
              {reviewResult && (
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <span className="badge" style={{ background: 'rgba(99, 102, 241, 0.2)', color: '#a5b4fc', border: '1px solid rgba(99, 102, 241, 0.3)' }}>
                    Total: {reviewResult.totalIssuesCount}
                  </span>
                  {reviewResult.skippedCount > 0 && (
                    <span className="badge badge-skipped-memory">
                      Skipped: {reviewResult.skippedCount}
                    </span>
                  )}
                  <span className="badge" style={{ background: 'rgba(244, 63, 94, 0.2)', color: '#fda4af', border: '1px solid rgba(244, 63, 94, 0.3)' }}>
                    Actionable: {reviewResult.activeIssuesCount}
                  </span>
                </div>
              )}
            </div>

            {/* Recalled Memory Insight Banner */}
            {reviewResult?.recalledMemories?.length > 0 && (
              <div className="memory-banner">
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontWeight: 700, marginBottom: '0.25rem', color: '#e9d5ff' }}>
                  <Brain size={16} /> Recalled Team Preferences from Hindsight ({reviewResult.recalledMemories.length}):
                </div>
                <ul style={{ paddingLeft: '1.2rem', marginTop: '0.25rem', fontSize: '0.825rem' }}>
                  {reviewResult.recalledMemories.map((mem, i) => (
                    <li key={i}>{mem}</li>
                  ))}
                </ul>
              </div>
            )}

            {!reviewResult && !loading && (
              <div style={{ textAlign: 'center', padding: '5rem 2rem', color: 'var(--text-muted)' }}>
                <Brain size={48} style={{ opacity: 0.2, margin: '0 auto 1rem' }} />
                <p style={{ fontWeight: 600, color: 'var(--text-main)' }}>No review submitted yet</p>
                <p style={{ fontSize: '0.85rem', marginTop: '0.25rem' }}>
                  Select a Java scenario on the left or paste your own code, then click <strong>Review Code with Memory</strong>.
                </p>
              </div>
            )}

            {loading && (
              <div style={{ textAlign: 'center', padding: '5rem 2rem', color: 'var(--text-muted)' }}>
                <div style={{ width: '40px', height: '40px', border: '3px solid rgba(168, 85, 247, 0.2)', borderTopColor: '#a855f7', borderRadius: '50%', animation: 'spin 1s linear infinite', margin: '0 auto 1rem' }} />
                <style>{`@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }`}</style>
                <p style={{ fontWeight: 600, color: 'var(--text-main)' }}>Checking Hindsight memory bank...</p>
                <p style={{ fontSize: '0.85rem', marginTop: '0.25rem' }}>Recalling team rules and analyzing Java semantics with Groq</p>
              </div>
            )}

            {/* List of comments */}
            {reviewResult?.comments?.map((comment) => {
              const isSkipped = comment.skippedDueToMemory;
              const status = feedbackStatus[comment.id];

              return (
                <div
                  key={comment.id}
                  id={`comment-${comment.id}`}
                  className={`comment-card ${isSkipped ? 'skipped' : ''}`}
                >
                  <div className="comment-header">
                    <div>
                      {isSkipped ? (
                        <span className="badge badge-skipped-memory">
                          <CheckCircle2 size={13} /> {comment.memoryReason || 'Skipped — Team Preference Applied'}
                        </span>
                      ) : (
                        <span className={`badge badge-${comment.severity || 'medium'}`}>
                          {comment.severity || 'Medium'} Severity
                        </span>
                      )}
                    </div>
                    {comment.ruleKey && (
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-subtle)', fontFamily: 'var(--font-mono)' }}>
                        #{comment.ruleKey}
                      </span>
                    )}
                  </div>

                  <div className="comment-issue">
                    {comment.issue}
                  </div>

                  {comment.suggestion && (
                    <div className="comment-suggestion">
                      <strong style={{ color: '#93c5fd' }}>Suggested Action:</strong> {comment.suggestion}
                    </div>
                  )}

                  {/* Feedback Controls (only if not already skipped) */}
                  {!isSkipped && (
                    <div>
                      {status ? (
                        <div style={{ 
                          fontSize: '0.8rem', 
                          padding: '0.5rem 0.75rem', 
                          borderRadius: '6px', 
                          background: status.decision === 'rejected' ? 'rgba(168, 85, 247, 0.2)' : 'rgba(16, 185, 129, 0.2)',
                          color: status.decision === 'rejected' ? '#e9d5ff' : '#6ee7b7',
                          border: '1px solid rgba(255,255,255,0.1)',
                          display: 'flex',
                          alignItems: 'center',
                          gap: '0.4rem'
                        }}>
                          <Brain size={14} /> {status.message}
                        </div>
                      ) : (
                        <div className="feedback-actions">
                          <input
                            type="text"
                            className="note-input"
                            placeholder="Optional reason (e.g. 'We allow this in legacy payment modules')"
                            value={notesByComment[comment.id] || ''}
                            onChange={(e) => setNotesByComment({ ...notesByComment, [comment.id]: e.target.value })}
                          />
                          <button
                            id={`btn-reject-${comment.id}`}
                            className="btn-reject"
                            onClick={() => handleFeedback(comment, 'rejected')}
                            title="Reject this suggestion and teach Hindsight to skip it on future reviews"
                          >
                            <X size={14} /> Reject & Remember
                          </button>
                          <button
                            id={`btn-accept-${comment.id}`}
                            className="btn-accept"
                            onClick={() => handleFeedback(comment, 'accepted')}
                            title="Accept and reinforce this convention"
                          >
                            <Check size={14} /> Accept Fix
                          </button>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}
