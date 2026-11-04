import { useEffect, useMemo, useState } from 'react';
import DataTable from './components/DataTable';
import EditRowModal from './components/EditRowModal';
import HistoryPanel from './components/HistoryPanel';
import SettingsPanel from './components/SettingsPanel';
import { generateLabels, getHistory, getSettings, previewLabels, pdfUrl, updateSettings } from './services/api';
import { parsePastedData } from './utils/parser';

const SIDEBAR_COLLAPSED_W = 72;
const SIDEBAR_EXPANDED_W = 260;


export default function App() {
  const [activeTab, setActiveTab] = useState('generate');
  const [inputText, setInputText] = useState('');
  const [rows, setRows] = useState([]);
  const [selectedIds, setSelectedIds] = useState(new Set());
  const [settings, setSettings] = useState(null);
  const [previewRow, setPreviewRow] = useState(null);
  const [editingRow, setEditingRow] = useState(null);
  const [history, setHistory] = useState([]);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState(null);
  const [markPrinted, setMarkPrinted] = useState(true);

  // Sidebar Drawer State (Hover + Lock)
  const [isSidebarHovered, setIsSidebarHovered] = useState(false);
  const [isSidebarLocked, setIsSidebarLocked] = useState(() => {
    try { return localStorage.getItem('alto_shaam_sidebar_locked') === 'true'; } catch { return false; }
  });

  const sidebarExpanded = isSidebarLocked || isSidebarHovered;

  const toggleSidebarLock = () => {
    const next = !isSidebarLocked;
    setIsSidebarLocked(next);
    try { localStorage.setItem('alto_shaam_sidebar_locked', String(next)); } catch { /* ignore */ }
  };

  useEffect(() => {
    loadSettings();
    loadHistory();
  }, []);

  async function loadSettings() {
    try {
      setSettings(await getSettings());
    } catch (error) {
      setMessage({ type: 'error', text: `Backend unreachable: ${error.message}` });
    }
  }

  async function loadHistory() {
    try {
      setHistory(await getHistory());
    } catch {
      // Non-blocking
    }
  }



  function clearAll() {
    setInputText('');
    setRows([]);
    setSelectedIds(new Set());
    setPreviewRow(null);
    setEditingRow(null);
    setMessage(null);
  }

  async function processData() {
    setMessage(null);
    try {
      setBusy(true);
      const parsed = parsePastedData(inputText);
      const response = await previewLabels(parsed);
      const returnedRows = response.rows.map((row, index) => ({
        ...parsed[index],
        ...row
      }));
      setRows(returnedRows);
      setSelectedIds(new Set(returnedRows.map(row => row.clientRowId)));
      setPreviewRow(returnedRows[0] || null);

      if (!response.canGenerate) {
        setMessage({ type: 'warning', text: 'Missing required configuration. Review Settings before generating.' });
      } else {
        setMessage({ type: 'success', text: `${returnedRows.length} rows processed and ready.` });
      }
    } catch (error) {
      setMessage({ type: 'error', text: error.message });
    } finally {
      setBusy(false);
    }
  }

  async function refreshRows(nextRows) {
    const response = await previewLabels(nextRows);
    const merged = response.rows.map((row, index) => ({ ...nextRows[index], ...row }));
    setRows(merged);
    if (previewRow) setPreviewRow(merged.find(row => row.clientRowId === previewRow.clientRowId) || merged[0]);
    return merged;
  }

  function toggleRow(id) {
    setSelectedIds(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  }

  function toggleAll() {
    setSelectedIds(prev => {
      const all = rows.every(row => prev.has(row.clientRowId));
      return all ? new Set() : new Set(rows.map(row => row.clientRowId));
    });
  }

  async function changePrintQuantity(id, value) {
    const quantity = value === '' ? null : Math.max(1, Number(value));
    const nextRows = rows.map(row => row.clientRowId === id ? { ...row, requestedPrintQuantity: Number.isFinite(quantity) ? Math.trunc(quantity) : null } : row);
    setRows(nextRows);
    try {
      await refreshRows(nextRows);
    } catch {
      // Local edit fallback
    }
  }

  function changePackageId(id, value) {
    setRows(prevRows =>
      prevRows.map(row =>
        row.clientRowId === id
          ? { ...row, pkgId: value, packageId: value }
          : row
      )
    );
  }

  async function saveEditedRow(nextRow) {
    const nextRows = rows.map(row => row.clientRowId === nextRow.clientRowId ? nextRow : row);
    try {
      setBusy(true);
      await refreshRows(nextRows);
      setEditingRow(null);
      setMessage({ type: 'success', text: 'Row updated.' });
    } catch (error) {
      setMessage({ type: 'error', text: error.message });
    } finally {
      setBusy(false);
    }
  }

  const selectableRows = useMemo(() => rows.filter(row => selectedIds.has(row.clientRowId)), [rows, selectedIds]);
  const totalRequested = useMemo(() => rows.reduce((sum, row) => sum + (row.requestedPrintQuantity || 0), 0), [rows]);

  async function handleGenerate() {
    if (!rows.length) {
      setMessage({ type: 'error', text: 'Please process data first.' });
      return;
    }
    if (!selectableRows.length) {
      setMessage({ type: 'error', text: 'Select at least one row to generate.' });
      return;
    }

    const pdfWindow = window.open('about:blank', '_blank');
    try {
      setBusy(true);
      const response = await generateLabels(selectableRows, markPrinted);
      setMessage({ type: 'success', text: `${response.physicalLabels} physical labels generated. Batch: ${response.batchNumber}.` });
      await loadHistory();
      setActiveTab('history');
      if (pdfWindow) {
        pdfWindow.location.href = pdfUrl(response.batchId);
      }
    } catch (error) {
      if (pdfWindow) pdfWindow.close();
      setMessage({ type: 'error', text: error.message });
    } finally {
      setBusy(false);
    }
  }

  async function saveSettings(next) {
    try {
      const saved = await updateSettings(next);
      setSettings(saved);
      setMessage({ type: 'success', text: 'Configuration saved successfully.' });
    } catch (error) {
      setMessage({ type: 'error', text: error.message });
      throw error;
    }
  }

  const viewMetadata = {
    generate: { title: 'Master Label Generation' },
    history: { title: 'Print History & Ledger' },
    settings: { title: 'Master Configuration' }
  };

  return (
    <div className="enterprise-app-layout">
      {/* ═══ ENTERPRISE SIDEBAR ═══ */}
      <aside
        className="enterprise-sidebar"
        onMouseEnter={() => setIsSidebarHovered(true)}
        onMouseLeave={() => setIsSidebarHovered(false)}
        style={{ width: sidebarExpanded ? SIDEBAR_EXPANDED_W : SIDEBAR_COLLAPSED_W }}
      >
        <div
          className="enterprise-sidebar-logo-block"
          style={{
            padding: sidebarExpanded ? '10px 14px 8px' : '10px 6px',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            boxSizing: 'border-box'
          }}
        >
          {/* Logo Container Badge: Full logo when expanded, outer "a" icon when closed */}
          <div
            style={{
              backgroundColor: '#ffffff',
              borderRadius: '8px',
              padding: sidebarExpanded ? '4px 8px' : '4px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: sidebarExpanded ? '100%' : '44px',
              maxWidth: sidebarExpanded ? '200px' : '44px',
              height: '42px',
              overflow: 'hidden',
              boxShadow: '0 2px 4px rgba(0, 0, 0, 0.12)',
              boxSizing: 'border-box',
              transition: 'all 200ms ease'
            }}
          >
            {sidebarExpanded ? (
              <img
                src="/autocrat-logo.png"
                alt="Autocrat Engineers"
                style={{
                  width: '100%',
                  height: '100%',
                  objectFit: 'contain',
                  transform: 'scale(1.5)',
                  transformOrigin: 'center',
                  display: 'block'
                }}
              />
            ) : (
              <img
                src="/outer.png"
                alt="Autocrat"
                style={{
                  width: '32px',
                  height: '32px',
                  objectFit: 'contain',
                  display: 'block'
                }}
              />
            )}
          </div>

          {/* MASTER LABEL SYSTEM Title (MLS when collapsed) */}
          <p
            style={{
              color: 'rgba(255, 255, 255, 0.95)',
              fontSize: sidebarExpanded ? '11px' : '10px',
              fontWeight: 700,
              letterSpacing: '0.05em',
              margin: '6px 0 4px 0',
              textAlign: 'center',
              whiteSpace: 'nowrap'
            }}
          >
            {sidebarExpanded ? 'MASTER LABEL SYSTEM' : 'MLS'}
          </p>

          {/* Compact Lock/Unlock Button */}
          {sidebarExpanded && (
            <button
              onClick={toggleSidebarLock}
              title={isSidebarLocked ? 'Click to unlock sidebar' : 'Click to lock sidebar open'}
              aria-label={isSidebarLocked ? 'Unlock sidebar' : 'Lock sidebar'}
              style={{
                background: isSidebarLocked ? 'rgba(255, 255, 255, 0.22)' : 'rgba(255, 255, 255, 0.08)',
                border: '1px solid rgba(255, 255, 255, 0.25)',
                color: '#ffffff',
                borderRadius: '5px',
                width: '26px',
                height: '24px',
                padding: 0,
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                cursor: 'pointer',
                transition: 'all 150ms ease'
              }}
            >
              {isSidebarLocked ? (
                <svg
                  width="13"
                  height="13"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2.2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                  <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                </svg>
              ) : (
                <svg
                  width="13"
                  height="13"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2.2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                  <path d="M7 11V7a5 5 0 0 1 9.9-1" />
                </svg>
              )}
            </button>
          )}
        </div>

        <nav className="enterprise-sidebar-nav">
          <button
            className={`enterprise-nav-item ${activeTab === 'generate' ? 'active' : ''}`}
            onClick={() => setActiveTab('generate')}
            style={{
              padding: sidebarExpanded ? '12px 20px' : '12px 0',
              justifyContent: sidebarExpanded ? 'flex-start' : 'center',
              gap: sidebarExpanded ? '12px' : '0px'
            }}
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2" />
              <path d="M6 9V3a1 1 0 0 1 1-1h10a1 1 0 0 1 1 1v6" />
              <rect x="6" y="14" width="12" height="8" rx="1" />
            </svg>
            {sidebarExpanded && (
              <div>
                <div style={{ fontWeight: 600 }}>Generate Labels</div>
                <div style={{ fontSize: '11px', color: 'var(--enterprise-gray-500)' }}>Batch paste & preview</div>
              </div>
            )}
          </button>
          <button
            className={`enterprise-nav-item ${activeTab === 'history' ? 'active' : ''}`}
            onClick={() => setActiveTab('history')}
            style={{
              padding: sidebarExpanded ? '12px 20px' : '12px 0',
              justifyContent: sidebarExpanded ? 'flex-start' : 'center',
              gap: sidebarExpanded ? '12px' : '0px'
            }}
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8" />
              <path d="M3 3v5h5" />
              <path d="M12 7v5l4 2" />
            </svg>
            {sidebarExpanded && (
              <div>
                <div style={{ fontWeight: 600 }}>Print History</div>
                <div style={{ fontSize: '11px', color: 'var(--enterprise-gray-500)' }}>Audit trail & reprints</div>
              </div>
            )}
          </button>

          <button
            className={`enterprise-nav-item ${activeTab === 'settings' ? 'active' : ''}`}
            onClick={() => setActiveTab('settings')}
            style={{
              padding: sidebarExpanded ? '12px 20px' : '12px 0',
              justifyContent: sidebarExpanded ? 'flex-start' : 'center',
              gap: sidebarExpanded ? '12px' : '0px'
            }}
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z" />
              <circle cx="12" cy="12" r="3" />
            </svg>
            {sidebarExpanded && (
              <div>
                <div style={{ fontWeight: 600 }}>Settings</div>
                <div style={{ fontSize: '11px', color: 'var(--enterprise-gray-500)' }}>Supplier configuration</div>
              </div>
            )}
          </button>
        </nav>
      </aside>

      {/* ═══ MAIN APPARATUS ═══ */}
      <div className="enterprise-main-content" style={{ marginLeft: sidebarExpanded ? SIDEBAR_EXPANDED_W : SIDEBAR_COLLAPSED_W }}>
        <header className="enterprise-header">
          <div className="enterprise-view-title">
            <h1>{viewMetadata[activeTab].title}</h1>
            <p>{viewMetadata[activeTab].desc}</p>
          </div>
          
        </header>

        {message && (
          <div className={`enterprise-banner ${message.type}`}>
            <span>{message.text}</span>
          </div>
        )}

        <div className="enterprise-page-body">
          {/* GENERATE TAB */}
          {activeTab === 'generate' && (
            <main style={{ width: '100%', boxSizing: 'border-box' }}>
              <section className="enterprise-panel">
                <div className="enterprise-panel-header">
                  <div>
                    <h2>1. Paste Shipment Manifest Data</h2>
                  </div>
                  <div style={{ display: 'flex', gap: '8px' }}>
                    <button className="enterprise-btn-secondary" onClick={clearAll}>Clear</button>
                  </div>
                </div>

                <textarea
                  className="enterprise-textarea"
                  value={inputText}
                  onChange={event => setInputText(event.target.value)}
                  placeholder="Paste tab-separated Excel rows here..."
                  spellCheck="false"
                />

                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '12px' }}>
                  <span style={{ fontSize: '12px', color: 'var(--enterprise-gray-500)' }}>
                    {inputText.trim()
                      ? `${inputText.split(/\r?\n/).filter(Boolean).length - 1} data rows detected`
                      : 'Awaiting pasted range'}
                  </span>
                  <button
                    className="enterprise-btn-primary"
                    disabled={busy || !inputText.trim()}
                    onClick={processData}
                  >
                    {busy ? 'Validating...' : 'Process Data'}
                  </button>
                </div>
              </section>

              {rows.length > 0 && (
                <section className="enterprise-panel">
                  <div className="enterprise-panel-header">
                    <div>
                      <h2>2. Review & Quantity Validation</h2>
                    </div>
                    <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
                      <span className="enterprise-role-badge" style={{ backgroundColor: 'var(--enterprise-gray-100)', color: 'var(--enterprise-gray-700)' }}>
                        {rows.length} Rows
                      </span>
                      <span className="enterprise-role-badge">
                        {totalRequested} Requested
                      </span>
                    </div>
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
                    <span style={{ fontSize: '13px', fontWeight: 600, color: 'var(--enterprise-gray-700)' }}>
                      {selectableRows.length} Rows Selected for Batch Print
                    </span>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                      <button
                        className="enterprise-btn-primary"
                        disabled={busy || !selectableRows.length}
                        onClick={handleGenerate}
                      >
                        {busy ? 'Generating PDF...' : 'Generate Master Labels'}
                      </button>
                    </div>
                  </div>

                  <DataTable
                    rows={rows}
                    selectedIds={selectedIds}
                    onToggle={toggleRow}
                    onToggleAll={toggleAll}
                    onEdit={setEditingRow}
                    onPreview={setPreviewRow}
                    onPrintQuantityChange={changePrintQuantity}
                    onPackageIdChange={changePackageId}
                  />
                </section>
              )}
            </main>
          )}

          {/* HISTORY TAB */}
          {activeTab === 'history' && (
            <main style={{ width: '100%', boxSizing: 'border-box' }}>
              <HistoryPanel history={history} onRefresh={loadHistory} />
            </main>
          )}

          {/* SETTINGS TAB */}
          {activeTab === 'settings' && (
            <main style={{ width: '100%', boxSizing: 'border-box' }}>
              <SettingsPanel settings={settings} onSave={saveSettings} />
            </main>
          )}
        </div>
      </div>

      {/* Edit Modal */}
      {editingRow && (
        <EditRowModal
          row={editingRow}
          onClose={() => setEditingRow(null)}
          onSave={saveEditedRow}
        />
      )}
    </div>
  );
}