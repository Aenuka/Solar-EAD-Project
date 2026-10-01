import { ArrowRight, Leaf, ShieldCheck, Sun, Users } from "lucide-react";
import {
  ActionLink,
  Badge,
  Brand,
  Field,
  Form,
  Heading,
  Hidden,
  Pagination,
  Save,
  Stat,
  Table,
  dateTime,
  path,
  usePortal,
} from "../components";

export function Login() {
  const { data } = usePortal();
  return (
    <div className="grid overflow-hidden rounded-[2rem] border border-slate-200/70 bg-white shadow-sm lg:grid-cols-2">
      <div className="relative flex min-h-80 flex-col justify-between overflow-hidden bg-gradient-to-br from-sky-50 via-sky-100/70 to-emerald-100 p-8 sm:p-12">
        <Brand />
        <div className="relative z-10 my-14">
          <p className="eyebrow">A brighter connection</p>
          <h1 className="max-w-sm text-4xl leading-tight sm:text-5xl">
            Clean energy.
            <br />
            Connected people.
          </h1>
          <p className="mt-5 max-w-sm leading-relaxed text-slate-600">
            One workspace for your solar community, from everyday connections to
            a more sustainable tomorrow.
          </p>
        </div>
        <div className="flex items-center gap-2 text-sm text-emerald-800">
          <Leaf size={18} /> Powered by a shared future
        </div>
        <Sun
          size={220}
          strokeWidth={0.5}
          className="pointer-events-none absolute -bottom-8 -right-12 text-sky-500/15"
          aria-hidden="true"
        />
      </div>
      <div className="flex flex-col justify-center p-8 sm:p-14">
        <p className="eyebrow">Staff portal</p>
        <h2 className="text-3xl">Welcome back.</h2>
        <p className="muted mb-8 mt-3">Sign in to manage your microgrid.</p>
        <Form action="/Account/Login">
          <Field
            label="Username"
            name="Username"
            value={data.model?.username}
            autoComplete="username"
            maxLength={50}
            required
          />
          <Field
            label="Password"
            name="Password"
            type="password"
            autoComplete="current-password"
            maxLength={128}
            required
          />
          <Save>
            Sign in <ArrowRight size={16} />
          </Save>
        </Form>
        <p className="muted mt-8 flex items-center gap-2 text-xs">
          <ShieldCheck size={16} /> Secure access for Backoffice and Grid
          Operators
        </p>
      </div>
    </div>
  );
}

export function Overview() {
  const { data } = usePortal();
  const operator = data.user.role === "GridOperator";
  const m = data.model || {};
  return (
    <>
      <Heading
        eyebrow={
          operator ? "Grid operator workspace" : "Your community, at a glance"
        }
        title={
          operator
            ? `Welcome, ${data.user.name.split(" ")[0]}.`
            : "A brighter overview."
        }
        description="Your people, your stations, and the energy that connects them."
      >
        {!operator && (
          <a className="btn" href="/Staff/Create">
            Add staff member
          </a>
        )}
      </Heading>
      {!operator && (
        <div className="mb-7 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <Stat
            label="Active prosumers"
            value={m.activeProsumers}
            note="Connected to your community"
            href="/Prosumers?status=Active"
            icon={Leaf}
          />
          <Stat
            label="Pending requests"
            value={m.pendingRequests}
            note="Ready for your review"
            href="/Prosumers?pending=true"
            icon={Sun}
          />
          <Stat
            label="Inactive prosumers"
            value={m.inactiveProsumers}
            note="Available for reactivation"
            href="/Prosumers?status=Inactive"
            icon={Users}
          />
          <Stat
            label="Team members"
            value={m.staffUsers}
            note="Backoffice & Grid Operators"
            href="/Staff"
            icon={ShieldCheck}
          />
        </div>
      )}
      <div className="grid gap-6 lg:grid-cols-5">
        <section className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-sky-100 to-emerald-100 p-8 sm:p-10 lg:col-span-3">
          <p className="eyebrow">Energy, together</p>
          <h2 className="max-w-sm text-3xl leading-tight">
            Small connections.
            <br />A more sustainable future.
          </h2>
          <p className="mb-8 mt-4 max-w-sm text-sm leading-relaxed text-slate-600">
            Keep your microgrid running smoothly. Manage station schedules and
            make clean energy available to your community.
          </p>
          <a href="/Stations" className="btn">
            Explore stations <ArrowRight size={16} />
          </a>
          <Leaf
            size={150}
            strokeWidth={0.7}
            className="pointer-events-none absolute -bottom-5 -right-4 text-emerald-600/15"
            aria-hidden="true"
          />
        </section>
        <section className="card lg:col-span-2">
          <h2>Workspace essentials</h2>
          <ActionLink href="/Bookings/Pending" title="Review pending bookings">
            Keep energy reservations moving.
          </ActionLink>
          {!operator && (
            <>
              <ActionLink
                href="/Prosumers?pending=true"
                title="Review deactivation requests"
              >
                Help members manage their participation.
              </ActionLink>
              <ActionLink href="/Staff" title="Manage team access">
                Keep roles and account status up to date.
              </ActionLink>
            </>
          )}
          {operator && (
            <ActionLink href="/Bookings/Dashboard" title="Booking dashboard">
              View recent activity and reservation counts.
            </ActionLink>
          )}
        </section>
      </div>
    </>
  );
}

export function Staff() {
  const { data } = usePortal();
  const m = data.model;
  if (data.page === "Index")
    return (
      <>
        <Heading
          eyebrow="People & permissions"
          title="Team & access"
          description={`${m.total} staff accounts across your microgrid.`}
        >
          <a className="btn" href="/Staff/Create">
            Add staff member
          </a>
        </Heading>
        <section className="card overflow-hidden !p-0">
          <Table
            headings={["Team member", "Username", "Role", "Status", "Actions"]}
            empty={!m.items.length && "No staff accounts found"}
          >
            {m.items.map((u) => (
              <tr key={u.id}>
                <td>
                  <strong>{u.fullName}</strong>
                  <span className="muted block">{u.email}</span>
                </td>
                <td>
                  {u.username}
                  {u.isProtected && (
                    <span className="ml-2 text-xs text-slate-500">
                      Protected
                    </span>
                  )}
                </td>
                <td>{u.role === "GridOperator" ? "Grid Operator" : u.role}</td>
                <td>
                  <Badge>{u.status}</Badge>
                </td>
                <td>
                  <a className="text-link" href={path("Staff", "Edit", u.id)}>
                    Manage →
                  </a>
                </td>
              </tr>
            ))}
          </Table>
        </section>
        <Pagination model={m} />
      </>
    );
  const create = data.page === "Create";
  const locked = m.isProtected || m.id === data.user.id;
  return (
    <>
      <a className="text-link mb-6" href="/Staff">
        ← Team & access
      </a>
      <Heading
        title={create ? "Add a team member." : m.username}
        description="The right access for the people behind your microgrid."
      />
      <section className="card max-w-2xl">
        <Form
          action={path("Staff", create ? "Create" : "Edit", m.id)}
          version={create ? null : m.version}
        >
          {create && (
            <Field
              label="Username"
              name="Username"
              value={m.username}
              pattern="[a-zA-Z0-9._\-]{3,50}"
              autoComplete="off"
              required
            />
          )}
          <div className="form-grid">
            <Field
              label="Full name"
              name="FullName"
              value={m.fullName}
              minLength={2}
              maxLength={100}
              required
            />
            <Field
              label="Email address"
              name="Email"
              type="email"
              value={m.email}
              maxLength={254}
              required
            />
          </div>
          {create && (
            <Field
              label="Temporary password"
              name="Password"
              type="password"
              minLength={12}
              maxLength={128}
              autoComplete="new-password"
              required
            >
              Use at least 12 characters.
            </Field>
          )}
          {!create && locked ? (
            <>
              <Hidden name="Role" value={m.role} />
              <Hidden name="Status" value={m.status} />
              <p className="rounded-2xl bg-sky-50 p-4 text-sm text-sky-900">
                Role: {m.role} · Status: {m.status}. Access settings for your
                own account and the protected administrator are locked.
              </p>
            </>
          ) : (
            <div className="form-grid">
              <Field
                label="Role"
                name="Role"
                value={m.role || "GridOperator"}
                options={[["GridOperator", "Grid Operator"], "Backoffice"]}
                required
              />
              {!create && (
                <Field
                  label="Account status"
                  name="Status"
                  value={m.status}
                  options={["Active", "Inactive"]}
                  required
                />
              )}
            </div>
          )}
          {!create && !locked && (
            <p className="muted">
              Changing role or status ends this member’s existing sessions.
            </p>
          )}
          <div className="flex flex-wrap items-center gap-4">
            <Save>{create ? "Create account" : "Save changes"}</Save>
            <a href="/Staff" className="text-link">
              Cancel
            </a>
            {!create && (
              <a className="text-link" href={path("Staff", "Edit", m.id)}>
                Reload account
              </a>
            )}
          </div>
        </Form>
      </section>
    </>
  );
}

export function Prosumers() {
  const { data } = usePortal();
  const m = data.model;
  if (data.page === "Details") return <ProsumerDetails model={m} />;
  const pending = data.meta.Pending;
  return (
    <>
      <Heading
        eyebrow="Your energy community"
        title={pending ? "Deactivation requests" : "Prosumer accounts"}
        description={
          pending
            ? "Review requests and help members manage their participation."
            : "Find, update, and manage your community’s accounts."
        }
      >
        <Badge>{`${m.total} ${pending ? "pending" : "accounts"}`}</Badge>
      </Heading>
      <section className="card overflow-hidden !p-0">
        <form
          action="/Prosumers"
          method="get"
          className="flex flex-wrap items-end gap-4 p-6"
        >
          <Hidden name="pending" value={String(pending)} />
          <div className="min-w-52 flex-1">
            <Field
              label="Search by name or NIC"
              name="search"
              value={data.meta.Search}
              placeholder="Find a prosumer…"
              maxLength={100}
            />
          </div>
          {!pending && (
            <Field
              label="Account status"
              name="status"
              value={data.meta.Status}
              options={[["", "All statuses"], "Active", "Inactive"]}
            />
          )}
          <button className="btn btn-secondary" type="submit">
            Search
          </button>
          <a
            className="text-link pb-3"
            href={pending ? "/Prosumers?pending=true" : "/Prosumers"}
          >
            Clear
          </a>
        </form>
        <Table
          headings={[
            "Prosumer",
            "NIC",
            "Account status",
            "Latest request",
            "Actions",
          ]}
          empty={
            !m.items.length &&
            (pending ? "You’re all caught up" : "No accounts found")
          }
        >
          {m.items.map((u) => (
            <tr key={u.nic}>
              <td>
                <strong>{u.fullName}</strong>
                <span className="muted block">{u.email}</span>
              </td>
              <td className="font-mono text-xs">{u.nic}</td>
              <td>
                <Badge>{u.status}</Badge>
              </td>
              <td>
                {u.deactivationRequest ? (
                  <Badge>{u.deactivationRequest.status}</Badge>
                ) : (
                  "—"
                )}
              </td>
              <td>
                <a
                  className="text-link"
                  href={path("Prosumers", "Details", u.nic)}
                >
                  {pending ? "Review" : "Manage"} →
                </a>
              </td>
            </tr>
          ))}
        </Table>
      </section>
      <Pagination model={m} />
    </>
  );
}

function ProsumerDetails({ model: m }) {
  const a = m.account;
  const request = a.deactivationRequest;
  return (
    <>
      <a href="/Prosumers" className="text-link mb-6">
        ← Prosumer accounts
      </a>
      <Heading
        title={a.fullName}
        description={`NIC ${a.nic} · Joined ${dateTime(a.createdAt)} (Sri Lanka)`}
      >
        <Badge>{a.status}</Badge>
      </Heading>
      <div className="grid items-start gap-6 lg:grid-cols-2">
        <section className="card">
          <h2 className="mb-2">Profile details</h2>
          <p className="muted mb-6">
            NIC is the account identifier and cannot be changed here.
          </p>
          <Form
            action={path("Prosumers", "Details", a.nic)}
            version={m.version}
          >
            <Field
              name="FullName"
              label="Full name"
              value={m.fullName}
              minLength={2}
              maxLength={100}
              required
            />
            <Field
              name="Email"
              label="Email address"
              type="email"
              value={m.email}
              maxLength={254}
              required
            />
            <Field
              name="Phone"
              label="Phone number"
              type="tel"
              value={m.phone}
              required
            />
            <Field
              name="Address"
              label="Address"
              type="textarea"
              value={m.address}
              minLength={5}
              maxLength={300}
              required
            />
            <div className="flex items-center gap-4">
              <Save>Save profile</Save>
              <a
                className="text-link"
                href={path("Prosumers", "Details", a.nic)}
              >
                Reload account
              </a>
            </div>
          </Form>
        </section>
        <div className="space-y-6">
          {request && (
            <section className="card">
              <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
                <h2>Deactivation request</h2>
                <Badge>{request.status}</Badge>
              </div>
              <p className="muted">
                Requested {dateTime(request.requestedAt)} (Sri Lanka)
              </p>
              <blockquote className="my-5 border-l-2 border-sky-200 pl-4 text-sm leading-relaxed">
                {request.reason}
              </blockquote>
              {request.status === "Pending" ? (
                <>
                  <p className="muted mb-5">
                    Approval makes this account inactive and ends the prosumer’s
                    existing sessions.
                  </p>
                  <Form
                    action={path("Prosumers", "Decide", a.nic)}
                    version={a.version}
                  >
                    <Hidden name="RequestId" value={request.id} />
                    <Field
                      label="Decision"
                      name="Decision"
                      options={[
                        ["", "Choose a decision"],
                        ["Approved", "Approve deactivation"],
                        ["Rejected", "Reject request"],
                      ]}
                      required
                    />
                    <Field
                      label="Review note"
                      name="Note"
                      type="textarea"
                      minLength={5}
                      maxLength={500}
                      required
                    />
                    <Save>Submit decision</Save>
                  </Form>
                </>
              ) : (
                <>
                  <p className="my-3 text-sm">{request.decisionNote}</p>
                  <p className="muted">
                    Reviewed {dateTime(request.reviewedAt)} (Sri Lanka)
                  </p>
                </>
              )}
            </section>
          )}
          {a.status === "Inactive" && (
            <section className="card">
              <h2>Reconnect this account</h2>
              <p className="muted my-4">
                The prosumer can sign in again with their existing credentials.
              </p>
              <Form
                action={path("Prosumers", "Reactivate", a.nic)}
                version={a.version}
              >
                <Field
                  name="Note"
                  label="Reactivation note"
                  type="textarea"
                  minLength={5}
                  maxLength={500}
                  required
                />
                <Save>Reactivate account</Save>
              </Form>
            </section>
          )}
          <section className="card">
            <h2 className="mb-5">Recent account activity</h2>
            <ol className="space-y-5">
              {[...a.recentEvents]
                .reverse()
                .slice(0, 10)
                .map((e, i) => (
                  <li key={i} className="border-l-2 border-emerald-100 pl-4">
                    <h3>{e.action.replace(/([a-z])([A-Z])/g, "$1 $2")}</h3>
                    <p className="muted text-xs">
                      {dateTime(e.at)} (Sri Lanka)
                    </p>
                    {e.note && <p className="mt-2 text-sm">{e.note}</p>}
                  </li>
                ))}
            </ol>
          </section>
        </div>
      </div>
    </>
  );
}
