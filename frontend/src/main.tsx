import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router";
import { App } from "./App";
import "./app.css";
import { initAnalytics } from "./lib/analytics";

initAnalytics();

const rootElement = document.getElementById("root");
if (rootElement === null) {
  throw new Error("#root element not found");
}

createRoot(rootElement).render(
  <StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </StrictMode>,
);
