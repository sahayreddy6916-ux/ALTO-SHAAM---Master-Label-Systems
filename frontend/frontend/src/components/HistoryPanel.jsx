import { pdfUrl } from '../services/api';

export default function HistoryPanel({ history, onRefresh }) {
  return (
    <section className="enterprise-panel" style={{ width: '100%', boxSizing: 'border-box' }}>
      {/* ── Header ── */}
      <div
        className="enterprise-panel-header"
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          paddingBottom: '16px',
          borderBottom: '1px solid var(--border-color)',
          marginBottom: '20px'
        }}
      >
        <div>
          <h2 style={{ fontSize: '18px', fontWeight: 700, margin: 0, color: 'var(--enterprise-gray-900)' }}>
            Audited Print Batches
          </h2>
        </div>
      </div>

      {/* ── Batch List ── */}
      {history.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '48px', color: 'var(--enterprise-gray-400)', fontSize: '14px' }}>
          No label batches have been recorded yet.
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          {history.map(batch => (
            <div
              key={batch.id}
              style={{
                border: '1px solid var(--border-color)',
                borderRadius: '8px',
                padding: '16px 20px',
                backgroundColor: '#ffffff',
                boxShadow: '0 1px 2px rgba(0, 0, 0, 0.03)'
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' }}>
                <div>
                  <div style={{ fontWeight: 700, fontSize: '15px', fontFamily: 'monospace', color: 'var(--enterprise-primary)' }}>
                    Batch: {batch.batchNumber}
                  </div>
                  <div style={{ fontSize: '12px', color: 'var(--enterprise-gray-500)', marginTop: '4px' }}>
                    {new Date(batch.createdAt).toLocaleString()}
                  </div>
                </div>

                <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
                  <a
                    className="enterprise-btn-secondary"
                    href={pdfUrl(batch.id)}
                    target="_blank"
                    rel="noreferrer"
                    style={{ padding: '6px 14px' }}
                  >
                    View PDF
                  </a>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}