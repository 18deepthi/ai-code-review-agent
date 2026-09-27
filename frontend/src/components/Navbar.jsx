import React from 'react';
import { Brain, Code2, History, Database, Sparkles, CheckCircle2, AlertCircle } from 'lucide-react';

export default function Navbar({ activeTab, setActiveTab, teamId, setTeamId, health }) {
  return (
    <header className="navbar">
      <div className="navbar-brand">
        <div className="brand-icon-wrapper">
          <Brain size={24} color="#fff" />
        </div>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
            <span>Hindsight Reviewer</span>
            <span style={{ 
              fontSize: '0.65rem', 
              background: 'linear-gradient(135deg, #a855f7, #6366f1)', 
              padding: '0.15rem 0.5rem', 
              borderRadius: '999px',
              fontWeight: 700 
            }}>AI MEMORY AGENT</span>
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 400 }}>
            Learns & remembers team preferences via Hindsight
          </div>
        </div>
      </div>

      <nav className="navbar-nav">
        <button
          id="nav-tab-submit"
          className={`nav-tab-btn ${activeTab === 'submit' ? 'active' : ''}`}
          onClick={() => setActiveTab('submit')}
        >
          <Code2 size={18} />
          Submit & Review
        </button>

        <button
          id="nav-tab-memory"
          className={`nav-tab-btn ${activeTab === 'memory' ? 'active' : ''}`}
          onClick={() => setActiveTab('memory')}
        >
          <Database size={18} color="#c084fc" />
          Memory Dashboard
        </button>

        <button
          id="nav-tab-history"
          className={`nav-tab-btn ${activeTab === 'history' ? 'active' : ''}`}
          onClick={() => setActiveTab('history')}
        >
          <History size={18} />
          History
        </button>
      </nav>

      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
        <div className="navbar-team-badge" title="Team Memory Bank ID">
          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Bank:</span>
          <input
            id="team-id-input"
            className="team-input"
            value={teamId}
            onChange={(e) => setTeamId(e.target.value)}
            placeholder="team-id"
          />
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
          {health?.status === 'UP' ? (
            <span title="Backend connected" style={{ display: 'flex', alignItems: 'center', gap: '0.3rem', color: '#10b981' }}>
              <CheckCircle2 size={14} /> Backend Online
            </span>
          ) : (
            <span title="Backend status" style={{ display: 'flex', alignItems: 'center', gap: '0.3rem', color: '#f59e0b' }}>
              <AlertCircle size={14} /> Checking...
            </span>
          )}
        </div>
      </div>
    </header>
  );
}
