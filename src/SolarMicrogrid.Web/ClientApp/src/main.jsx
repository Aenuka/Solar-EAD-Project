import { Component } from "react";
import { createRoot } from "react-dom/client";
import App from "./App";
import "./styles.css";

class ErrorBoundary extends Component {
  state = { failed: false };
  static getDerivedStateFromError() {
    return { failed: true };
  }
  render() {
    return this.state.failed ? (
      <main className="card mx-auto mt-16 max-w-lg">
        <h1>Unable to display this page.</h1>
        <p className="muted my-5">
          Please reload to get the latest information.
        </p>
        <a
          className="btn"
          href={window.location.pathname + window.location.search}
        >
          Reload page
        </a>
      </main>
    ) : (
      this.props.children
    );
  }
}

const initialData = JSON.parse(
  document.getElementById("portal-data")?.textContent || "null",
);
createRoot(document.getElementById("root")).render(
  <ErrorBoundary>
    {initialData ? (
      <App initialData={initialData} />
    ) : (
      <main className="card mx-auto mt-16 max-w-lg">
        <h1>Solar Microgrid</h1>
        <a className="text-link mt-5" href="/Account/Login">
          Open the staff portal →
        </a>
      </main>
    )}
  </ErrorBoundary>,
);
