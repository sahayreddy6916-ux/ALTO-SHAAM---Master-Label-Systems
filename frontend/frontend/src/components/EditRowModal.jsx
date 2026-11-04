import { useEffect, useState } from 'react';

const SCENARIOS = [
  ['PALLET_IDENTICAL', 'Pallet - identical part/PO/packing list'],
  ['PALLET_MIXED', 'Mixed pallet - one Master Label per combination'],
  ['LARGE_CONTAINER_IDENTICAL', 'Large/bulk container - identical part'],
  ['LOOSE_CONTAINER', 'Loose container']
];

export default function EditRowModal({ row, onClose, onSave }) {
  const [form, setForm] = useState(row);

  useEffect(() => setForm(row), [row]);

  if (!row) return null;

  function update(field, value) {
    setForm(prev => ({ ...prev, [field]: value }));
  }

  function numeric(field, value) {
    const cleaned = value === '' ? null : Number(value);
    update(field, Number.isFinite(cleaned) ? Math.trunc(cleaned) : null);
  }

  return (
    <div className="enterprise-modal-backdrop" onMouseDown={onClose}>
      <div className="enterprise-modal" onMouseDown={e => e.stopPropagation()}>
        <div className="enterprise-modal-header">
          <div>
            <h3 style={{ margin: 0, fontSize: '16px', fontWeight: 700, color: 'var(--enterprise-gray-900)' }}>
              Edit Master Label Details
            </h3>
            <p style={{ margin: '3px 0 0', fontSize: '12px', color: 'var(--enterprise-gray-500)' }}>
              Overrides are validated against the backend sequence logic.
            </p>
          </div>
          <button
            onClick={onClose}
            style={{ border: 'none', background: 'none', fontSize: '20px', cursor: 'pointer', color: 'var(--enterprise-gray-400)' }}
          >
            ×
          </button>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '14px' }}>
          <div>
            <label style={{ display: 'block', fontSize: '11px', fontWeight: 700, color: 'var(--enterprise-gray-700)', marginBottom: '4px' }}>
              CUSTOMER PO
            </label>
            <input
              style={{ width: '100%', padding: '8px 10px', border: '1px solid var(--border-color)', borderRadius: '6px', fontSize: '12px' }}
              value={form.customerPoNumber || ''}
              onChange={e => update('customerPoNumber', e.target.value)}
            />
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '11px', fontWeight: 700, color: 'var(--enterprise-gray-700)', marginBottom: '4px' }}>
              PART NUMBER
            </label>
            <input
              style={{ width: '100%', padding: '8px 10px', border: '1px solid var(--border-color)', borderRadius: '6px', fontSize: '12px' }}
              value={form.partNumber || ''}
              onChange={e => update('partNumber', e.target.value)}
            />
          </div>

          <div style={{ gridColumn: '1 / -1' }}>
            <label style={{ display: 'block', fontSize: '11px', fontWeight: 700, color: 'var(--enterprise-gray-700)', marginBottom: '4px' }}>
              DESCRIPTION
            </label>
            <textarea
              rows="2"
              style={{ width: '100%', padding: '8px 10px', border: '1px solid var(--border-color)', borderRadius: '6px', fontSize: '12px' }}
              value={form.description || ''}
              onChange={e => update('description', e.target.value)}
            />
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '11px', fontWeight: 700, color: 'var(--enterprise-gray-700)', marginBottom: '4px' }}>
              PER PALLET QUANTITY
            </label>
            <input
              type="number"
              min="1"
              style={{ width: '100%', padding: '8px 10px', border: '1px solid var(--border-color)', borderRadius: '6px', fontSize: '12px' }}
              value={form.perPalletQuantity ?? ''}
              onChange={e => numeric('perPalletQuantity', e.target.value)}
            />
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '11px', fontWeight: 700, color: 'var(--enterprise-gray-700)', marginBottom: '4px' }}>
              TOTAL QUANTITY
            </label>
            <input
              type="number"
              min="1"
              style={{ width: '100%', padding: '8px 10px', border: '1px solid var(--border-color)', borderRadius: '6px', fontSize: '12px' }}
              value={form.totalQuantity ?? ''}
              onChange={e => numeric('totalQuantity', e.target.value)}
            />
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '11px', fontWeight: 700, color: 'var(--enterprise-gray-700)', marginBottom: '4px' }}>
              NUMBER OF PALLETS
            </label>
            <input
              type="number"
              min="1"
              style={{ width: '100%', padding: '8px 10px', border: '1px solid var(--border-color)', borderRadius: '6px', fontSize: '12px' }}
              value={form.numberOfPallets ?? ''}
              onChange={e => numeric('numberOfPallets', e.target.value)}
            />
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '11px', fontWeight: 700, color: 'var(--enterprise-gray-700)', marginBottom: '4px' }}>
              PRINT QUANTITY OVERRIDE
            </label>
            <input
              type="number"
              min="1"
              style={{ width: '100%', padding: '8px 10px', border: '1px solid var(--border-color)', borderRadius: '6px', fontSize: '12px' }}
              value={form.requestedPrintQuantity ?? ''}
              onChange={e => numeric('requestedPrintQuantity', e.target.value)}
            />
          </div>

          <div style={{ gridColumn: '1 / -1' }}>
            <label style={{ display: 'block', fontSize: '11px', fontWeight: 700, color: 'var(--enterprise-gray-700)', marginBottom: '4px' }}>
              SHIPPING SCENARIO
            </label>
            <select
              style={{ width: '100%', padding: '8px 10px', border: '1px solid var(--border-color)', borderRadius: '6px', fontSize: '12px' }}
              value={form.shippingScenario || 'PALLET_IDENTICAL'}
              onChange={e => update('shippingScenario', e.target.value)}
            >
              {SCENARIOS.map(([val, label]) => (
                <option key={val} value={val}>{label}</option>
              ))}
            </select>
          </div>
        </div>

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '22px' }}>
          <button className="enterprise-btn-secondary" onClick={onClose}>Cancel</button>
          <button className="enterprise-btn-primary" onClick={() => onSave(form)}>Save Changes</button>
        </div>
      </div>
    </div>
  );
}