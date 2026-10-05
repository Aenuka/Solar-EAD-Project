import { createContext, useContext, useId, useRef, useState } from "react";
import { ArrowRight, CheckCircle2, CircleAlert, Sun, X } from "lucide-react";

export const PortalContext = createContext(null);
export const usePortal = () => useContext(PortalContext);
export const path = (area, action, id) =>
  `/${area}/${action}${id ? `/${encodeURIComponent(id)}` : ""}`;
export const dateTime = (value) =>
  value
    ? new Intl.DateTimeFormat("en-GB", {
        dateStyle: "medium",
        timeStyle: "short",
        timeZone: "Asia/Colombo",
      }).format(new Date(value))
    : "—";

export function Brand() {
  return (
    <a
      href="/"
      className="inline-flex shrink-0 items-center gap-2.5 font-semibold tracking-tight text-slate-800"
    >
      <span className="flex size-8 items-center justify-center text-slate-800">
        <Sun size={27} strokeWidth={1.7} aria-hidden="true" />
      </span>
      <span className="text-base">Solar Microgrid</span>
    </a>
  );
}

export function Heading({ eyebrow, title, description, children }) {
  return (
    <div className="page-heading">
      <div>
        <p className="eyebrow">{eyebrow || "Solar Microgrid"}</p>
        <h1>{title}</h1>
        {description && (
          <p className="mt-4 max-w-2xl text-base leading-relaxed text-slate-500">
            {description}
          </p>
        )}
      </div>
      {children}
    </div>
  );
}

export function Badge({ children }) {
  const status = String(children).toLowerCase();
  const color = ["active", "approved", "completed"].includes(status)
    ? "bg-emerald-50 text-emerald-700"
    : status === "pending"
      ? "bg-amber-50 text-amber-800"
      : "bg-slate-100 text-slate-600";
  return (
    <span
      className={`inline-flex items-center gap-1.5 whitespace-nowrap rounded-full px-3 py-1 text-xs font-medium ${color}`}
    >
      <span className="size-1.5 rounded-full bg-current" aria-hidden="true" />
      {children}
    </span>
  );
}

export function Notice({ children, error = false }) {
  if (!children) return null;
  const Icon = error ? CircleAlert : CheckCircle2;
  return (
    <div
      role={error ? "alert" : "status"}
      className={`mb-5 flex items-start gap-3 rounded-2xl p-4 text-sm ${error ? "bg-red-50 text-red-800" : "bg-emerald-50 text-emerald-800"}`}
    >
      <Icon className="mt-0.5 shrink-0" size={18} aria-hidden="true" />
      <div>{children}</div>
    </div>
  );
}

export function Field({
  label,
  name,
  value,
  type = "text",
  options,
  children,
  ...props
}) {
  const id = useId();
  const { data } = usePortal();
  const errors = Object.entries(data.errors || {})
    .filter(([key]) => key.toLowerCase() === name.toLowerCase())
    .flatMap(([, messages]) => messages);
  const attributes = {
    id,
    name,
    defaultValue: value ?? "",
    "aria-invalid": errors.length > 0 || undefined,
    "aria-describedby": errors.length ? `${id}-error` : undefined,
    ...props,
  };
  return (
    <div className="space-y-2">
      <label htmlFor={id} className="block text-sm font-medium text-slate-700">
        {label}
      </label>
      {options ? (
        <select {...attributes}>
          {options.map((option) => {
            const [v, text] = Array.isArray(option) ? option : [option, option];
            return (
              <option key={v} value={v}>
                {text}
              </option>
            );
          })}
        </select>
      ) : type === "textarea" ? (
        <textarea {...attributes} rows={3} />
      ) : (
        <input {...attributes} type={type} />
      )}
      {children && <p className="muted text-xs">{children}</p>}
      {errors.length > 0 && (
        <p id={`${id}-error`} className="text-xs text-red-700">
          {errors.join(" ")}
        </p>
      )}
    </div>
  );
}

export const Hidden = ({ name, value }) => (
  <input type="hidden" name={name} value={value ?? ""} />
);

export function Form({
  action,
  version,
  children,
  confirm,
  className = "space-y-5",
}) {
  const { data, submit, busy } = usePortal();
  return (
    <form
      action={action}
      method="post"
      className={className}
      onSubmit={(event) => {
        event.preventDefault();
        if (busy || (confirm && !window.confirm(confirm))) return;
        submit(action, new FormData(event.currentTarget));
      }}
    >
      <Hidden name="__RequestVerificationToken" value={data.csrfToken} />
      {version != null && <Hidden name="Version" value={version} />}
      <fieldset disabled={busy} className="min-w-0 space-y-5">
        {children}
      </fieldset>
    </form>
  );
}

export function Save({ children = "Save changes" }) {
  const { busy } = usePortal();
  return (
    <button className="btn" type="submit" disabled={busy}>
      {busy ? "Saving…" : children}
    </button>
  );
}

export function Empty({ title = "Nothing here yet", children }) {
  return (
    <div className="py-16 text-center">
      <Sun className="mx-auto mb-4 text-sky-500" size={32} aria-hidden="true" />
      <h2>{title}</h2>
      <p className="muted mx-auto mt-2 max-w-md">{children}</p>
    </div>
  );
}

export function Table({ headings, children, empty }) {
  const hintId = useId();
  return empty ? (
    <Empty title={empty} />
  ) : (
    <div>
      <p id={hintId} className="px-6 py-3 text-xs text-slate-500 sm:hidden">
        Scroll horizontally to see all columns.
      </p>
      <div
        className="overflow-x-auto"
        tabIndex={0}
        role="region"
        aria-label="Records"
        aria-describedby={hintId}
      >
        <table className="w-full">
          <thead>
            <tr>
              {headings.map((h) => (
                <th key={h} scope="col">
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>{children}</tbody>
        </table>
      </div>
    </div>
  );
}

export function Pagination({ model }) {
  const link = (page) => {
    const params = new URLSearchParams(window.location.search);
    params.set("page", page);
    return `${window.location.pathname}?${params}`;
  };
  return (
    <nav
      aria-label="Pagination"
      className="mt-5 flex flex-wrap items-center justify-between gap-4 text-sm text-slate-500"
    >
      <span>
        Page {model.page} · {model.total} records
      </span>
      <div className="flex gap-4">
        {model.page > 1 && (
          <a className="text-link" href={link(model.page - 1)}>
            ← Previous
          </a>
        )}
        {model.page * model.pageSize < model.total && (
          <a className="text-link" href={link(model.page + 1)}>
            Next →
          </a>
        )}
      </div>
    </nav>
  );
}

export function Stat({ label, value, note, href, icon: Icon = Sun }) {
  const Tag = href ? "a" : "div";
  return (
    <Tag href={href} className="stat-card group">
      <div className="mb-5 flex items-center justify-between gap-4 text-sm font-medium text-slate-500">
        {label}
        <Icon
          size={19}
          strokeWidth={1.5}
          className="text-slate-400"
          aria-hidden="true"
        />
      </div>
      <div className="text-5xl font-semibold tracking-[-0.04em]">
        {value ?? 0}
      </div>
      <p className="muted mt-3">{note}</p>
    </Tag>
  );
}

export function ActionLink({ href, title, children }) {
  return (
    <a
      href={href}
      className="flex items-center justify-between gap-4 border-b border-slate-100 py-5 last:border-0"
    >
      <span>
        <span className="block text-sm font-semibold">{title}</span>
        <span className="muted mt-1 block">{children}</span>
      </span>
      <ArrowRight
        size={18}
        className="shrink-0 text-sky-600"
        aria-hidden="true"
      />
    </a>
  );
}

export function Modal({ title, trigger, children }) {
  const ref = useRef(null);
  const button = useRef(null);
  const titleId = useId();
  return (
    <>
      <button
        ref={button}
        className="btn"
        type="button"
        aria-haspopup="dialog"
        onClick={() => ref.current.showModal()}
      >
        {trigger}
      </button>
      <dialog
        ref={ref}
        aria-labelledby={titleId}
        onClose={() => button.current?.focus()}
      >
        <div className="mb-5 flex items-center justify-between">
          <h2 id={titleId}>{title}</h2>
          <button
            type="button"
            aria-label="Close dialog"
            className="rounded-full p-2 hover:bg-slate-100"
            onClick={() => ref.current.close()}
          >
            <X size={20} />
          </button>
        </div>
        {children}
        <button
          className="text-link mt-5"
          type="button"
          onClick={() => ref.current.close()}
        >
          Cancel
        </button>
      </dialog>
    </>
  );
}

export function Expand({ label, children }) {
  const [open, setOpen] = useState(false);
  const id = useId();
  return (
    <div>
      <button
        type="button"
        className="btn btn-secondary"
        aria-expanded={open}
        aria-controls={id}
        onClick={() => setOpen(!open)}
      >
        {open ? "Cancel editing" : label}
      </button>
      {open && (
        <div id={id} className="mt-5">
          {children}
        </div>
      )}
    </div>
  );
}
