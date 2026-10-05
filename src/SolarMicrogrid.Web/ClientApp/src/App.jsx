/**
 * @author Aenuka Buddhakorala
 * IT number: IT23214934
 * File functionality:
 * - Renders the React portal, submits authenticated forms, and selects pages and navigation based on the current user's role.
 */

import { useEffect, useRef, useState } from "react";
import {
  ClipboardList,
  LayoutDashboard,
  Leaf,
  LogOut,
  Menu,
  Sun,
  Users,
  X,
} from "lucide-react";
import { Brand, Form, Heading, Notice, PortalContext } from "./components";
import { Login, Overview, Prosumers, Staff } from "./pages/Accounts";
import { Stations } from "./pages/Stations";
import { Bookings } from "./pages/Bookings";
import { Operator } from "./pages/Operator";

export default function App({ initialData }) {
  const [data, setData] = useState(initialData);
  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState("");
  const [revision, setRevision] = useState(0);
  const submitting = useRef(false);
  const noticeRef = useRef(null);

  useEffect(() => {
    document.title = `${data.page === "Login" ? "Sign in" : data.controller === "Home" ? "Overview" : data.controller} · Solar Microgrid`;
  }, [data]);

  async function submit(action, body) {
    if (submitting.current) return;
    submitting.current = true;
    setBusy(true);
    setFailure("");
    try {
      const response = await fetch(action, {
        method: "POST",
        body,
        credentials: "same-origin",
        headers: { Accept: "application/json" },
      });
      if (!response.headers.get("content-type")?.includes("application/json")) {
        throw new Error(
          response.status === 400
            ? "This form has expired. Reload the page and try again."
            : "The service could not complete your request. Reload the page before trying again.",
        );
      }
      const result = await response.json();
      if (!result.csrfToken || !result.page)
        throw new Error(
          "Unexpected response. Reload the page before trying again.",
        );
      if (response.redirected) {
        const url = new URL(response.url);
        window.history.replaceState(null, "", url.pathname + url.search);
      }
      setData(result);
      setRevision((r) => r + 1);
    } catch (error) {
      setFailure(
        error.message ||
          "Connection lost. Check the current record before retrying.",
      );
    } finally {
      submitting.current = false;
      setBusy(false);
      requestAnimationFrame(() => {
        noticeRef.current?.focus();
        noticeRef.current?.scrollIntoView?.({
          behavior: window.matchMedia?.("(prefers-reduced-motion: reduce)")
            ?.matches
            ? "auto"
            : "smooth",
          block: "start",
        });
      });
    }
  }

  const errors = [...new Set(Object.values(data.errors || {}).flat())];
  const notices = (
    <div ref={noticeRef} tabIndex={-1} className="outline-none">
      <Notice error>{failure}</Notice>
      <Notice error>{data.notices?.error || data.meta?.Error}</Notice>
      {errors.length > 0 && (
        <Notice error>
          <ul className="list-inside list-disc">
            {errors.map((error) => (
              <li key={error}>{error}</li>
            ))}
          </ul>
        </Notice>
      )}
      <Notice>{data.notices?.success}</Notice>
      <Notice>{data.notices?.info}</Notice>
      {data.meta?.Expired && (
        <Notice error>Your session expired. Please sign in again.</Notice>
      )}
    </div>
  );
  return (
    <PortalContext.Provider value={{ data, submit, busy }}>
      <a
        href="#main"
        className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded-xl focus:bg-white focus:p-4"
      >
        Skip to content
      </a>
      {data.user ? (
        <Shell data={data}>
          <main id="main" className="portal-main" aria-busy={busy}>
            {notices}
            <Page key={revision} data={data} />
          </main>
        </Shell>
      ) : (
        <main
          id="main"
          className="mx-auto max-w-[1100px] px-5 py-8 sm:px-8 sm:py-16"
          aria-busy={busy}
        >
          {notices}
          <Page key={revision} data={data} />
          <p className="mt-7 text-center text-xs text-slate-400">
            Solar Microgrid · A connected energy community
          </p>
        </main>
      )}
    </PortalContext.Provider>
  );
}

function Shell({ data, children }) {
  const [menuOpen, setMenuOpen] = useState(false);
  const menuButton = useRef(null);
  useEffect(() => {
    if (!menuOpen) return;
    const closeOnEscape = (event) => {
      if (event.key === "Escape") {
        setMenuOpen(false);
        menuButton.current?.focus();
      }
    };
    document.addEventListener("keydown", closeOnEscape);
    return () => document.removeEventListener("keydown", closeOnEscape);
  }, [menuOpen]);
  const pending = data.controller === "Prosumers" && data.meta?.Pending;
  const links = [
    ["Home", "/", "Overview", LayoutDashboard],
    ["Stations", "/Stations", "Stations", Sun],
    ["Bookings", "/Bookings", "Bookings", ClipboardList],
    ...(data.user.role === "Backoffice"
      ? [
          ["Prosumers", "/Prosumers", "Prosumers", Leaf],
          ["Requests", "/Prosumers?pending=true", "Requests", ClipboardList],
          ["Staff", "/Staff", "Team & access", Users],
        ]
      : []),
  ];
  return (
    <>
      <header className="portal-header">
        <div className="global-bar">
          <Brand />
          <nav
            id="main-navigation"
            aria-label="Main navigation"
            className={`portal-nav ${menuOpen ? "flex" : "hidden"} lg:flex`}
          >
            {links.map(([key, href, label, Icon]) => {
              const active =
                key === "Requests"
                  ? pending
                  : (key === data.controller ||
                      (key === "Home" && data.controller === "Operator")) &&
                    !pending;
              return (
                <a
                  key={key}
                  href={href}
                  aria-current={active ? "page" : undefined}
                  className="portal-nav-link"
                  onClick={() => setMenuOpen(false)}
                >
                  <Icon className="lg:hidden" size={18} aria-hidden="true" />
                  {label}
                </a>
              );
            })}
          </nav>
          <div className="flex shrink-0 items-center gap-2">
            <div className="hidden max-w-36 items-center text-right xl:flex">
              <p
                className="truncate text-xs font-medium"
                title={data.user.name}
              >
                {data.user.name}
              </p>
            </div>
            <Form action="/Account/Logout">
              <button
                type="submit"
                className="icon-button"
                aria-label="Sign out"
                title="Sign out"
              >
                <LogOut size={18} />
              </button>
            </Form>
            <button
              ref={menuButton}
              type="button"
              className="icon-button lg:hidden"
              aria-label={menuOpen ? "Close navigation" : "Open navigation"}
              aria-expanded={menuOpen}
              aria-controls="main-navigation"
              onClick={() => setMenuOpen(!menuOpen)}
            >
              {menuOpen ? <X /> : <Menu />}
            </button>
          </div>
        </div>
      </header>
      {children}
      <footer className="portal-footer">
        <span>Solar Microgrid</span>
        <span className="flex items-center gap-1.5">
          <Leaf size={13} /> A connected energy community
        </span>
      </footer>
    </>
  );
}

function Page({ data }) {
  if (data.page === "Error" || data.page.includes("/Error"))
    return (
      <section className="card">
        <Heading
          title={`Something needs attention. (${data.model.statusCode})`}
          description={data.model.message}
        />
        <a className="btn" href={data.user ? "/" : "/Account/Login"}>
          {data.user ? "Back to overview" : "Back to sign in"}
        </a>
      </section>
    );
  if (data.controller === "Account" && data.page === "Login") return <Login />;
  if (data.controller === "Home") return <Overview />;
  if (data.controller === "Operator") return <Operator />;
  if (data.controller === "Staff") return <Staff />;
  if (data.controller === "Prosumers") return <Prosumers />;
  if (data.controller === "Stations") return <Stations />;
  if (data.controller === "Bookings") return <Bookings />;
  return (
    <Notice error>
      This page could not be loaded. <a href="/">Return to overview</a>.
    </Notice>
  );
}
