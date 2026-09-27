import React, { useState, useEffect } from 'react';
import { Database, Brain, Sparkles, Filter, RefreshCw, Plus, Clock, Tag, FileText, CheckCircle2, XCircle } from 'lucide-react';
import { fetchTeamMemories, sendFeedback } from '../services/api';

export default function MemoryDashboardPage({ teamId }) {
  const [memories, setMemories] = useState([]);
  const [loading, setLoading] = useState(false);
  const [filter, setFilter] = useState('all'); // 'all', 'rejected', 'accepted'
  const [showAddForm, setShowAddForm] = useState(false);
  const [customRule, setCustomRule] = useState('');
  const [customNote, setCustomNote] = useState('');
  const [customDecision, setCustomDecision] = useState('rejected');
  const [savingRule, setSavingRule] = useState(false);

  const loadMemories = async () => {
    setLoading(true);
    try {
      const data = await fetchTeamMemories(teamId);
      setMemories(data || []);
    } catch (err) {
      console.error('Failed to load memories:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadMemories();
  }, [teamId]);

  const handleAddCustomRule = async (e) => {
    e.preventDefault();
    if (!customRule.trim()) return;

    setSavingRule(true);
    try {
      await sendFeedback({
        teamId,
        reviewId: 'manual-entry',
        commentId: 'custom-' + Date.now(),
        decision: customDecision,
        rule: customRule,
        note: customNote,
        sourceSnippetExcerpt: 'Manual policy specification'
      });
      setCustomRule('');
      setCustomNote('');
      setShowAddForm(false);
      await loadMemories();
    } catch (err) {
      alert('Failed to save rule: ' + err.message);
    } finally {
      setSavingRule(false);
    }
  };

  const overriddenCount = memories.filter((m) => m.decision === 'rejected').length;
  const enforcedCount = memories.filter((m) => m.decision === 'accepted').length;

  const filteredMemories = memories.filter((m) => {
    if (filter === 'all') return true;
    return m.decision === filter;
  });

  return (
    <div>
      <div className="page-header" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 className="page-title">
            <Database color="#c084fc" size={32} />
            Hindsight Memory Bank: <span style={{ color: '#a855f7' }}>{teamId}</span>
          </h1>
          <p className="page-subtitle">
            This dashboard displays the persistent long-term knowledge Hindsight has stored for your team.
            Every rejected suggestion becomes an architectural preference that guides all future reviews.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '0.75rem' }}>
          <button
            id="btn-refresh-memories"
            className="btn-secondary"
            onClick={loadMemories}
            disabled={loading}
          >
            <RefreshCw size={16} className={loading ? 'animate-spin' : ''} />
            Refresh Bank
          </button>
          <button
            id="btn-add-rule-toggle"
            className="btn-primary"
            onClick={() => setShowAddForm(!showAddForm)}
          >
            <Plus size={16} />
            Teach Team Rule
          </button>
        </div>
      </div>

      {/* Metrics Row */}
      <div className="stats-row">
        <div className="stat-card" style={{ borderColor: 'rgba(168, 85, 247, 0.4)', boxShadow: 'var(--glow-memory)' }}>
          <div className="stat-icon" style={{ background: 'rgba(168, 85, 247, 0.2)', color: '#c084fc' }}>
            <Brain size={24} />
          </div>
          <div>
            <div className="stat-value" style={{ color: '#e9d5ff' }}>{memories.length}</div>
            <div className="stat-label">Total Learned Preferences</div>
          </div>
        </div>

        <div className="stat-card" style={{ borderColor: 'rgba(244, 63, 94, 0.3)' }}>
          <div className="stat-icon" style={{ background: 'rgba(244, 63, 94, 0.2)', color: '#fda4af' }}>
            <XCircle size={24} />
          </div>
          <div>
            <div className="stat-value" style={{ color: '#fda4af' }}>{overriddenCount}</div>
            <div className="stat-label">Overridden Rules (Silently Skipped)</div>
          </div>
        </div>

        <div className="stat-card" style={{ borderColor: 'rgba(16, 185, 129, 0.3)' }}>
          <div className="stat-icon" style={{ background: 'rgba(16, 185, 129, 0.2)', color: '#6ee7b7' }}>
            <CheckCircle2 size={24} />
          </div>
          <div>
            <div className="stat-value" style={{ color: '#6ee7b7' }}>{enforcedCount}</div>
            <div className="stat-label">Reinforced Standards</div>
          </div>
        </div>
      </div>

      {/* Proactive Rule Entry Form Modal/Accordion */}
      {showAddForm && (
        <form onSubmit={handleAddCustomRule} className="glass-card" style={{ marginBottom: '2rem', borderColor: 'var(--border-memory)' }}>
          <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Sparkles size={18} color="#c084fc" /> Teach Agent a New Team Convention
          </h3>
          <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '1rem', marginBottom: '1rem' }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '0.35rem' }}>
                Convention / Preference Statement:
              </label>
              <input
                id="input-custom-rule"
                type="text"
                className="note-input"
                style={{ width: '100%', padding: '0.6rem 0.8rem', fontSize: '0.9rem' }}
                placeholder="e.g. Do not flag field injection in legacy payment integration services"
                value={customRule}
                onChange={(e) => setCustomRule(e.target.value)}
                required
              />
            </div>
            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '0.35rem' }}>
                Rule Type:
              </label>
              <select
                id="select-custom-decision"
                className="snippet-select-dropdown"
                style={{ width: '100%' }}
                value={customDecision}
                onChange={(e) => setCustomDecision(e.target.value)}
              >
                <option value="rejected">Overridden (Skip / Do Not Flag)</option>
                <option value="accepted">Enforced (Always Enforce)</option>
              </select>
            </div>
          </div>

          <div style={{ marginBottom: '1rem' }}>
            <label style={{ display: 'block', fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '0.35rem' }}>
              Justification / Architectural Context:
            </label>
            <input
              id="input-custom-note"
              type="text"
              className="note-input"
              style={{ width: '100%', padding: '0.6rem 0.8rem', fontSize: '0.9rem' }}
              placeholder="e.g. Migration to constructor injection planned for Q3; suppress warnings until then"
              value={customNote}
              onChange={(e) => setCustomNote(e.target.value)}
            />
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
            <button
              type="button"
              className="btn-secondary"
              onClick={() => setShowAddForm(false)}
            >
              Cancel
            </button>
            <button
              id="btn-save-rule-submit"
              type="submit"
              className="btn-primary"
              disabled={savingRule || !customRule.trim()}
            >
              {savingRule ? 'Writing to Hindsight...' : 'Commit to Hindsight Memory'}
            </button>
          </div>
        </form>
      )}

      {/* Filter Tabs */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.25rem' }}>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button
            className={`btn-secondary ${filter === 'all' ? 'active' : ''}`}
            onClick={() => setFilter('all')}
            style={filter === 'all' ? { borderColor: 'var(--accent-primary)', background: 'rgba(99, 102, 241, 0.15)' } : {}}
          >
            All Memories ({memories.length})
          </button>
          <button
            className={`btn-secondary ${filter === 'rejected' ? 'active' : ''}`}
            onClick={() => setFilter('rejected')}
            style={filter === 'rejected' ? { borderColor: 'var(--accent-purple)', background: 'rgba(168, 85, 247, 0.15)' } : {}}
          >
            Overridden Rules ({overriddenCount})
          </button>
          <button
            className={`btn-secondary ${filter === 'accepted' ? 'active' : ''}`}
            onClick={() => setFilter('accepted')}
            style={filter === 'accepted' ? { borderColor: 'var(--accent-emerald)', background: 'rgba(16, 185, 129, 0.15)' } : {}}
          >
            Enforced Standards ({enforcedCount})
          </button>
        </div>

        <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
          Bank: <code style={{ color: '#c084fc' }}>{teamId}</code>
        </span>
      </div>

      {/* Memories List */}
      <div>
        {filteredMemories.length === 0 && !loading && (
          <div className="glass-card" style={{ textAlign: 'center', padding: '4rem 2rem', color: 'var(--text-muted)' }}>
            <Brain size={48} style={{ opacity: 0.2, margin: '0 auto 1rem' }} />
            <p style={{ fontWeight: 600, color: 'var(--text-main)' }}>No memories recorded yet for team "{teamId}"</p>
            <p style={{ fontSize: '0.875rem', marginTop: '0.25rem' }}>
              Run code reviews and click "Reject & Remember" on any suggestion, or click <strong>Teach Team Rule</strong> above!
            </p>
          </div>
        )}

        {filteredMemories.map((mem) => {
          const isOverridden = mem.decision === 'rejected';
          return (
            <div key={mem.id} className="memory-item-card">
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                <span className={isOverridden ? 'badge badge-skipped-memory' : 'badge'} style={!isOverridden ? { background: 'rgba(16, 185, 129, 0.2)', color: '#6ee7b7' } : {}}>
                  {isOverridden ? 'OVERRIDDEN PREFERENCE (DO NOT FLAG)' : 'ENFORCED STANDARD'}
                </span>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-subtle)', display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
                  <Clock size={12} />
                  {mem.timestamp ? new Date(mem.timestamp).toLocaleString() : 'Recent'}
                </span>
              </div>

              <div className="memory-rule-title">
                {mem.rule}
              </div>

              {mem.note && (
                <div style={{ fontSize: '0.85rem', color: '#cbd5e1', marginTop: '0.35rem', background: 'rgba(0,0,0,0.2)', padding: '0.4rem 0.75rem', borderRadius: '4px' }}>
                  <strong style={{ color: '#c084fc' }}>Team Rationale:</strong> {mem.note}
                </div>
              )}

              {mem.sourceSnippetExcerpt && (
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)', marginTop: '0.5rem' }}>
                  Context Excerpt: <code>{mem.sourceSnippetExcerpt.replace(/\n/g, ' ').slice(0, 100)}...</code>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
