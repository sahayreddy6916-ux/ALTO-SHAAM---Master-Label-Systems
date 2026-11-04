const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8081/api";

async function parseResponse(response) {
  const contentType = response.headers.get("content-type") || "";

  if (contentType.includes("application/json")) {
    const body = await response.json();

    if (!response.ok) {
      const message =
        body.errors?.join("\n") ||
        body.message ||
        "Request failed";

      throw new Error(message);
    }

    return body;
  }

  if (!response.ok) {
    throw new Error(
      (await response.text()) || "Request failed"
    );
  }

  return response;
}

export async function getSettings() {
  return parseResponse(
    await fetch(`${API_BASE_URL}/settings`)
  );
}

export async function updateSettings(payload) {
  return parseResponse(
    await fetch(`${API_BASE_URL}/settings`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    })
  );
}

export async function previewLabels(rows) {
  return parseResponse(
    await fetch(`${API_BASE_URL}/master-labels/preview`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(rows),
    })
  );
}

export async function generateLabels(rows, markPrinted) {
  return parseResponse(
    await fetch(`${API_BASE_URL}/master-labels/generate`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        rows,
        markPrinted,
      }),
    })
  );
}

export async function getHistory() {
  return parseResponse(
    await fetch(
      `${API_BASE_URL}/master-labels/history?limit=50`
    )
  );
}

export async function getBatch(batchId) {
  return parseResponse(
    await fetch(
      `${API_BASE_URL}/master-labels/batches/${batchId}`
    )
  );
}

export async function markPrinted(batchId) {
  return parseResponse(
    await fetch(
      `${API_BASE_URL}/master-labels/batches/${batchId}/mark-printed`,
      {
        method: "PUT",
      }
    )
  );
}

export function pdfUrl(batchId) {
  return `${API_BASE_URL}/master-labels/batches/${batchId}/pdf`;
}

export function reprintUrl(batchId) {
  return `${API_BASE_URL}/master-labels/batches/${batchId}/reprint`;
}

export { API_BASE_URL };