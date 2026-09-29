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
          behavior: "smooth",
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
          <main
            id="main"
            className="mx-auto max-w-[1440px] px-5 py-9 sm:px-9 lg:px-12 lg:py-12"
            aria-busy={busy}
          >
            {notices}
            <Page key={revision} data={data} />
          </main>
        </Shell>
      ) : (
        <main
          id="main"
          className="mx-auto max-w-6xl px-5 py-8 sm:py-16"
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
      <header className="border-b border-slate-200/80 bg-white">
        <div className="mx-auto flex max-w-[1440px] items-center justify-between gap-4 px-5 py-5 sm:px-9 lg:px-12">
          <Brand />
          <div className="flex items-center gap-4">
            <div className="hidden text-right sm:block">
              <p className="text-sm font-medium">{data.user.name}</p>
              <p className="mt-0.5 text-xs text-slate-500">
                {data.user.role === "GridOperator"
                  ? "Grid Operator"
                  : "Backoffice"}
              </p>
            </div>
            <Form action="/Account/Logout">
              <button
                type="submit"
                className="flex size-10 items-center justify-center rounded-full bg-slate-100 text-slate-600 hover:bg-slate-200"
                aria-label="Sign out"
                title="Sign out"
              >
                <LogOut size={18} />
              </button>
            </Form>
            <button
              type="button"
              className="rounded-full p-2 md:hidden"
              aria-label={menuOpen ? "Close navigation" : "Open navigation"}
              aria-expanded={menuOpen}
              aria-controls="main-navigation"
              onClick={() => setMenuOpen(!menuOpen)}
            >
              {menuOpen ? <X /> : <Menu />}
            </button>
          </div>
        </div>
        <nav
          id="main-navigation"
          aria-label="Main navigation"
          className={`${menuOpen ? "flex" : "hidden"} mx-auto max-w-[1440px] flex-wrap gap-1 px-5 pb-3 sm:px-9 md:flex lg:px-12`}
        >
          {links.map(([key, href, label, Icon]) => {
            const active =
              key === "Requests"
                ? pending
                : key === data.controller && !pending;
            return (
              <a
                key={key}
                href={href}
                aria-current={active ? "page" : undefined}
                className={`flex items-center gap-2 rounded-full px-4 py-2.5 text-sm transition ${active ? "bg-sky-50 font-medium text-sky-700" : "text-slate-500 hover:bg-slate-50 hover:text-slate-800"}`}
              >
                <Icon size={16} aria-hidden="true" />
                {label}
              </a>
            );
          })}
        </nav>
      </header>
      {children}
      <footer className="mx-auto flex max-w-[1440px] flex-wrap justify-between gap-2 px-5 py-8 text-xs text-slate-400 sm:px-9 lg:px-12">
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
