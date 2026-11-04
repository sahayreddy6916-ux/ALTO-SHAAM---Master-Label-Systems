import { useEffect, useState } from 'react';

export default function SettingsPanel({ settings, onSave }) {
  const [form, setForm] = useState(settings || {});
  const [saving, setSaving] = useState(false);

  useEffect(() => setForm(settings || {}), [settings]);

  function update(field, value) {
    setForm(prev => ({ ...prev, [field]: value }));
  }

  async function submit(event) {
    event.preventDefault();
    setSaving(true);
    try {
      await onSave(form);
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="enterprise-panel">
      <div className="enterprise-panel-header">
        <div>
          <h2>Alto-Shaam Master Configuration</h2>
        </div>
        <span className="enterprise-role-badge">Format B-10</span>
      </div>

      <form onSubmit={submit} style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '16px' }}>
        <div>
          <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, marginBottom: '6px' }}>
            A-S Supplier ID *
          </label>
          <input
            required
            pattern="[0-9]{1,14}"
            style={{ width: '100%', padding: '8px 12px', border: '1px solid var(--border-color)', borderRadius: '6px', fontSize: '13px' }}
            value={form.supplierId || ''}
            onChange={e => update('supplierId', e.target.value)}
            placeholder="1-14 digits"
          />
          <span style={{ fontSize: '11px', color: 'var(--enterprise-gray-500)', display: 'block', marginTop: '4px' }}>
            The 15-digit serial sequence starts with this identifier.
          </span>
        </div>

        <div style={{ gridColumn: '1 / -1' }}>
          <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, marginBottom: '6px' }}>
            Ship From Address *
          </label>
          <textarea
            required
            rows="3"
            style={{ width: '100%', padding: '8px 12px', border: '1px solid var(--border-color)', borderRadius: '6px', fontSize: '13px' }}
            value={form.shipFrom || ''}
            onChange={e => update('shipFrom', e.target.value)}
          />
        </div>

        <div style={{ gridColumn: '1 / -1' }}>
          <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, marginBottom: '6px' }}>
            Ship To Address *
          </label>
          <textarea
            required
            rows="3"
            style={{ width: '100%', padding: '8px 12px', border: '1px solid var(--border-color)', borderRadius: '6px', fontSize: '13px' }}
            value={form.shipTo || ''}
            onChange={e => update('shipTo', e.target.value)}
          />
        </div>

        <div style={{ gridColumn: '1 / -1', display: 'flex', justifyContent: 'flex-end', marginTop: '10px' }}>
          <button className="enterprise-btn-primary" type="submit" disabled={saving}>
            {saving ? 'Saving...' : 'Save Configuration'}
          </button>
        </div>
      </form>
    </section>
  );
}