import { useEffect, useRef } from 'react';
import JsBarcode from 'jsbarcode';

export default function Barcode({ value, height = 48 }) {
  const ref = useRef(null);

  useEffect(() => {
    if (!ref.current || !value) return;
    try {
      JsBarcode(ref.current, value, {
        format: 'CODE128',
        displayValue: true,
        fontSize: 11,
        height,
        margin: 4,
        width: 1.35,
        textMargin: 3,
        background: '#ffffff',
        lineColor: '#111827'
      });
    } catch {
      if (ref.current) ref.current.innerHTML = '';
    }
  }, [value, height]);

  return <svg ref={ref} aria-label={`Code 128 ${value}`} className="barcode-svg" />;
}
