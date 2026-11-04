const HEADER_ALIASES = {
  customerPoNumber: ['cust po num', 'cust po', 'customer po', 'customer po num', 'purchase order', 'po number'],
  invoiceNumber: ['invoice num', 'invoice number', 'invoice'],
  packageId: ['pkg id', 'package id', 'pkg'],
  itemNumber: ['item no.', 'item no', 'item number', 'item'],
  partNumber: ['part no.', 'part no', 'part number', 'part'],
  description: ['item/service description', 'item service description', 'description', 'item description'],
  perPalletQuantity: ['per pallet qty', 'per pallet quantity', 'pallet qty'],
  totalQuantity: ['total quantity packed', 'total quantity', 'total qty packed', 'total qty'],
  numberOfPallets: ['no of pallets', 'number of pallets', 'pallets', 'no. of pallets'],
  requestedPrintQuantity: ['no of labels to be printed', 'number of labels to be printed', 'labels to print', 'print quantity', 'no of labels'],
  packingListOverride: ['packing list #', 'packing list number', 'packing list no', 'packing list'],
  revisionLevel: ['rev level', 'revision level', 'rev', 'revision', 'rev lvl', 'rev no', 'revision no']
};

function normalizeHeader(value) {
  return String(value ?? '')
    .trim()
    .toLowerCase()
    .replace(/[._]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function splitDelimitedLine(line, delimiter) {
  if (delimiter === '\t') return line.split('\t').map(cell => cell.trim());

  const cells = [];
  let current = '';
  let quote = false;
  for (let i = 0; i < line.length; i += 1) {
    const char = line[i];
    if (char === '"') {
      if (quote && line[i + 1] === '"') {
        current += '"';
        i += 1;
      } else {
        quote = !quote;
      }
    } else if (char === delimiter && !quote) {
      cells.push(current.trim());
      current = '';
    } else {
      current += char;
    }
  }
  cells.push(current.trim());
  return cells;
}

function detectDelimiter(firstLine) {
  if (firstLine.includes('\t')) return '\t';
  if (firstLine.includes('|')) return '|';
  return ',';
}

function isSeparatorRow(cells) {
  return cells.length > 0 && cells.every(cell => /^:?-{2,}:?$/.test(cell.trim()));
}

function findField(normalizedHeader) {
  for (const [field, aliases] of Object.entries(HEADER_ALIASES)) {
    if (aliases.includes(normalizedHeader)) return field;
  }
  return null;
}

function cleanPipeTable(cells) {
  const result = [...cells];
  if (result[0] === '') result.shift();
  if (result[result.length - 1] === '') result.pop();
  return result;
}

function toNumber(value) {
  const cleaned = String(value ?? '').replace(/,/g, '').trim();
  if (!cleaned) return null;
  const numeric = Number(cleaned);
  return Number.isFinite(numeric) ? Math.trunc(numeric) : null;
}

export function parsePastedData(text) {
  const rawLines = String(text ?? '').split(/\r?\n/).map(line => line.trim()).filter(Boolean);
  if (rawLines.length < 2) {
    throw new Error('Paste the header row and at least one data row.');
  }

  const delimiter = detectDelimiter(rawLines[0]);
  let headerCells = splitDelimitedLine(rawLines[0], delimiter);
  if (delimiter === '|') headerCells = cleanPipeTable(headerCells);

  const fieldMap = headerCells.map(cell => findField(normalizeHeader(cell)));
  const required = ['customerPoNumber', 'partNumber', 'description', 'perPalletQuantity', 'totalQuantity', 'numberOfPallets'];
  const missing = required.filter(field => !fieldMap.includes(field));
  if (missing.length) {
    throw new Error(`Missing required columns: ${missing.join(', ')}. Expected the standard Alto-Shaam input columns.`);
  }

  const rows = [];
  let dataRowNumber = 1;
  for (let i = 1; i < rawLines.length; i += 1) {
    let cells = splitDelimitedLine(rawLines[i], delimiter);
    if (delimiter === '|') cells = cleanPipeTable(cells);
    if (!cells.length || isSeparatorRow(cells)) continue;

    const get = field => {
      const index = fieldMap.indexOf(field);
      return index >= 0 ? (cells[index] ?? '') : '';
    };

    rows.push({
      clientRowId: `row-${dataRowNumber}`,
      customerPoNumber: get('customerPoNumber'),
      invoiceNumber: get('invoiceNumber'),
      packageId: get('packageId'),
      itemNumber: get('itemNumber'),
      partNumber: get('partNumber'),
      description: get('description'),
      perPalletQuantity: toNumber(get('perPalletQuantity')),
      totalQuantity: toNumber(get('totalQuantity')),
      numberOfPallets: toNumber(get('numberOfPallets')),
      requestedPrintQuantity: toNumber(get('requestedPrintQuantity')),
      packingListOverride: get('packingListOverride'),
      revisionLevel: get('revisionLevel'),
      shippingScenario: 'PALLET_IDENTICAL'
    });
    dataRowNumber += 1;
  }

  if (!rows.length) throw new Error('No data rows were found after parsing the paste.');
  return rows;
}

