const form = document.getElementById("convert-form");
const resultBox = document.getElementById("result");
const errorBox = document.getElementById("error");
const swapBtn = document.getElementById("swap");
const fromSelect = document.getElementById("from");
const toSelect = document.getElementById("to");

swapBtn.addEventListener("click", () => {
  const fromValue = fromSelect.value;
  fromSelect.value = toSelect.value;
  toSelect.value = fromValue;
});

form.addEventListener("submit", async (e) => {
  e.preventDefault();
  hideMessages();
  const amount = document.getElementById("amount").value;
  const from = fromSelect.value;
  const to = toSelect.value;
  try {
    const response = await fetch(`/api/convert?amount=${encodeURIComponent(amount)}&from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`);
    const contentType = response.headers.get("content-type") || "";
    const text = await response.text();
    let data = null;
    if (contentType.includes("application/json")) {
      data = JSON.parse(text);
    }

    if (!response.ok) {
      const message = data?.error || "Service indisponible. Réessayez dans un instant.";
      throw new Error(message);
    }
    if (!data) {
      throw new Error("Réponse inattendue du serveur");
    }

    const line = `${data.amount} ${data.from} = ${Number(data.converted).toFixed(2)} ${data.to}`;
    let detail;
    if (data.rateFrom && data.rateTo) {
      const forward = data.rateTo / data.rateFrom;
      const backward = data.rateFrom / data.rateTo;
      detail = `Taux: ${data.from}→${data.to} ≈ ${forward.toFixed(4)} | ${data.to}→${data.from} ≈ ${backward.toFixed(4)}`;
    } else {
      detail = "Taux: données indisponibles";
    }
    showResult(`${line}\n${detail}`);
  } catch (err) {
    showError(err.message);
  }
});

function hideMessages() {
  resultBox.classList.add("hidden");
  errorBox.classList.add("hidden");
}

function showResult(message) {
  resultBox.textContent = message;
  resultBox.classList.remove("hidden");
}

function showError(message) {
  errorBox.textContent = message;
  errorBox.classList.remove("hidden");
}
