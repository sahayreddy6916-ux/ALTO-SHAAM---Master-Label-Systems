export default function DataTable({
  rows,
  selectedIds,
  onToggle,
  onToggleAll,
  onEdit,
  onPackageIdChange,
  onPreview,
  onPrintQuantityChange
}) {
  const allSelected = rows.length > 0 && rows.every(row => selectedIds.has(row.clientRowId));

  return (
    <div className="enterprise-table-container">
      <table>
        <thead>
          <tr>
            <th style={{ width: '40px', textAlign: 'center' }}>
              <input
                type="checkbox"
                checked={allSelected}
                onChange={onToggleAll}
                aria-label="Select all rows"
              />
            </th>
            <th>Customer PO</th>
            <th>Part Number</th>
            <th>Description</th>
            <th>Pallet Qty</th>
            <th>Total Qty</th>
            <th>Pallets</th>
            <th style={{ width: '110px' }}>Print Qty</th>
            <th style={{ minWidth: '130px' }}>Package ID</th>
          </tr>
        </thead>
        <tbody>
          {rows.map(row => {
            const isSelected = selectedIds.has(row.clientRowId);

            return (
              <tr key={row.clientRowId} className={isSelected ? 'selected-row' : ''}>
                <td style={{ textAlign: 'center' }}>
                  <input
                    type="checkbox"
                    checked={isSelected}
                    onChange={() => onToggle(row.clientRowId)}
                  />
                </td>
                <td style={{ fontFamily: 'monospace', fontWeight: 600 }}>{row.customerPoNumber || '-'}</td>
                <td style={{ fontFamily: 'monospace', fontWeight: 600, color: 'var(--enterprise-primary)' }}>
                  {row.partNumber || '-'}
                </td>
                <td style={{ maxWidth: '300px', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }} title={row.description}>
                  {row.description || '-'}
                </td>
                <td>{row.perPalletQuantity ?? '-'}</td>
                <td>{row.totalQuantity ?? '-'}</td>
                <td>{row.numberOfPallets ?? '-'}</td>
                <td>
                  <input
                    type="number"
                    disabled
                    value={row.requestedPrintQuantity ?? ''}
                    style={{
                      width: '78px',
                      padding: '4px 8px',
                      border: '1px solid var(--border-color)',
                      borderRadius: '6px',
                      fontSize: '12px',
                      color: '#070808ff',
                    }}
                  />
                </td>
                <td>
                  <input
                    type="text"
                    placeholder="Enter PKG ID"
                    value={row.pkgId || row.packageId || ''}
                    onChange={event => onPackageIdChange(row.clientRowId, event.target.value)}
                    style={{
                      width: '100%',
                      maxWidth: '120px',
                      padding: '4px 8px',
                      border: '1px solid var(--border-color)',
                      borderRadius: '6px',
                      fontSize: '12px',
                      fontFamily: 'monospace'
                    }}
                  />
                </td>
                <td style={{ textAlign: 'center', whiteSpace: 'nowrap', paddingRight: '16px' }}>
                  <div style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}>


                  </div>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}