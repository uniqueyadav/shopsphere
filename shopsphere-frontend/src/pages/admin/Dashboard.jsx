import { useCallback, useEffect, useState } from "react";
import { motion } from "framer-motion";
import AdminLayout from "../../layouts/AdminLayout";
import StatsCard from "../../components/dashboard/StatsCard";
import ChartCard from "../../components/dashboard/ChartCard";
import DataTable from "../../components/dashboard/DataTable";
import { getAdminDashboard } from "../../services/adminService";
import { getRequestError } from "../../services/authService";

const currency = (amount) =>
  amount == null
    ? "—"
    : new Intl.NumberFormat("en-IN", {
        style: "currency",
        currency: "INR",
        maximumFractionDigits: 0,
      }).format(amount);
const number = (value) =>
  value == null ? "—" : new Intl.NumberFormat("en-IN").format(value);
const statusColors = {
  DELIVERED: "#20b99e",
  PROCESSING: "#ffb33f",
  CANCELLED: "#f36c72",
  SHIPPED: "#4288f5",
  OUT_FOR_DELIVERY: "#8c6cf5",
  PLACED: "#4b63ef",
  CONFIRMED: "#28a5cc",
  PAYMENT_PENDING: "#9ba6bb",
};

function OrderStatusPanel({ data, loading }) {
  const rows = data?.orderStatuses || [];
  const total = data?.totalOrders || 0;
  const shown = ["DELIVERED", "PROCESSING", "CANCELLED"].map((status) => ({
    status,
    count: rows.find((row) => row.status === status)?.count || 0,
  }));
  const others = rows.filter(
    (row) => !shown.some((item) => item.status === row.status),
  );
  let cursor = 0;
  const stops = rows.map((row) => {
    const from = cursor;
    cursor += total ? (row.count / total) * 100 : 0;
    return `${statusColors[row.status] || "#9ba6bb"} ${from}% ${cursor}%`;
  });
  return (
    <section className="admin-panel admin-status-panel">
      <div className="admin-panel-heading">
        <div>
          <span className="admin-heading-icon"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="m12 3 2.8 5.7 6.2.9-4.5 4.4 1.1 6.2L12 17.3l-5.6 2.9 1.1-6.2L3 9.6l6.2-.9L12 3Z"/></svg></span>
          <h2>Order Status</h2>
        </div>
      </div>
      {loading ? (
        <div className="admin-donut-loading" />
      ) : total === 0 ? (
        <div className="admin-inline-empty">
          Order status data is unavailable for this period.
        </div>
      ) : (
        <div className="admin-status-content">
          <div
            className="admin-donut"
            style={{ background: `conic-gradient(${stops.join(",")})` }}
          >
            <div className="admin-donut-center">
              <strong>{number(total)}</strong>
              <span>Total Orders</span>
            </div>
          </div>
          <div className="admin-status-legend">
            {[...shown, ...others].map((item) => (
              <div className="admin-status-legend-row" key={item.status}>
                <i
                  style={{ background: statusColors[item.status] || "#9ba6bb" }}
                />
                <span>{item.status.replaceAll("_", " ")}</span>
                <strong>
                  {total ? Math.round((item.count / total) * 100) : 0}%
                </strong>
              </div>
            ))}
          </div>
        </div>
      )}
    </section>
  );
}

function TopProducts({ products, loading }) {
  const max = Math.max(
    ...(products || []).map((product) => product.unitsSold || 0),
    1,
  );
  return (
    <section className="admin-panel admin-products-panel">
      <div className="admin-panel-heading">
        <div>
          <span className="admin-heading-icon"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="m12 3 2.8 5.7 6.2.9-4.5 4.4 1.1 6.2L12 17.3l-5.6 2.9 1.1-6.2L3 9.6l6.2-.9L12 3Z"/></svg></span>
          <h2>Top Selling Products</h2>
        </div>
        <span className="admin-view-all">By units sold</span>
      </div>
      {loading ? (
        <div className="admin-product-skeleton">
          {[0, 1, 2, 3].map((key) => (
            <i key={key} />
          ))}
        </div>
      ) : products?.length ? (
        <div className="admin-top-products">
          {products.map((product, index) => (
            <div className="admin-product-row" key={product.id}>
              <div className="admin-product-image">
                {product.imageUrl ? (
                  <img src={product.imageUrl} alt="" />
                ) : (
                  <span>{(product.name || "P").slice(0, 1)}</span>
                )}
              </div>
              <div className="admin-product-name" title={product.name}>
                {product.name}
              </div>
              <div className="admin-product-progress">
                <span
                  style={{
                    width: `${Math.max(8, (product.unitsSold / max) * 100)}%`,
                    background: [
                      "#5148f5",
                      "#4288f5",
                      "#20b99e",
                      "#ffb33f",
                      "#e875ad",
                    ][index % 5],
                  }}
                />
              </div>
              <div className="admin-product-units">
                {number(product.unitsSold)} sold
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="admin-inline-empty">
          No paid product sales found for this period.
        </div>
      )}
    </section>
  );
}

export default function Dashboard() {
  const [period, setPeriod] = useState("SIX_MONTHS");
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [exporting, setExporting] = useState(false);
  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setData(await getAdminDashboard(period));
    } catch (requestError) {
      setError(
        getRequestError(
          requestError,
          "Dashboard analytics could not be loaded.",
        ),
      );
      setData(null);
    } finally {
      setLoading(false);
    }
  }, [period]);
  useEffect(() => {
    load();
  }, [load]);
  const exportReport = () => {
    if (!data) return;
    setExporting(true);
    const rows = [
      ["Metric", "Value"],
      ["Revenue", data.revenue?.value ?? ""],
      ["Orders", data.orders?.value ?? ""],
      ["Customers", data.customers?.value ?? ""],
      ["Active sellers", data.activeSellers?.value ?? ""],
      [],
      ["Period", "Revenue", "Orders"],
      ...(data.trend || []).map((point) => [
        point.label,
        point.revenue,
        point.orders,
      ]),
    ];
    const csv = rows
      .map((row) =>
        row
          .map((cell) => `"${String(cell ?? "").replaceAll('"', '""')}"`)
          .join(","),
      )
      .join("\r\n");
    const link = document.createElement("a");
    link.href = URL.createObjectURL(
      new Blob([csv], { type: "text/csv;charset=utf-8" }),
    );
    link.download = `shopsphere-dashboard-${period.toLowerCase()}.csv`;
    link.click();
    URL.revokeObjectURL(link.href);
    setExporting(false);
  };
  const metrics = data
    ? [
        {
          title: "Total Revenue",
          value: currency(data.revenue?.value),
          growth: data.revenue?.growthPercent,
          icon: "revenue",
          color: "#5148f5",
          tone: "purple",
          trend: data.revenue?.sparkline,
        },
        {
          title: "Total Orders",
          value: number(data.orders?.value),
          growth: data.orders?.growthPercent,
          icon: "orders",
          color: "#20b99e",
          tone: "green",
          trend: data.orders?.sparkline,
        },
        {
          title: "Total Customers",
          value: number(data.customers?.value),
          growth: data.customers?.growthPercent,
          icon: "customers",
          color: "#fa8d29",
          tone: "orange",
          trend: data.customers?.sparkline,
        },
        {
          title: "Active Sellers",
          value: number(data.activeSellers?.value),
          growth: data.activeSellers?.growthPercent,
          icon: "sellers",
          color: "#3889f4",
          tone: "blue",
          trend: data.activeSellers?.sparkline,
        },
      ]
    : [];
  return (
    <AdminLayout
      onExport={exportReport}
      exporting={exporting}
      period={period}
      onPeriodChange={setPeriod}
    >
      <motion.div
        className="admin-dashboard-content"
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25 }}
      >
        {error && (
          <div className="admin-error-banner" role="alert">
            <span>{error}</span>
            <button type="button" onClick={load}>
              Try again
            </button>
          </div>
        )}
        <div className="admin-stats-grid">
          {loading
            ? [0, 1, 2, 3].map((key) => (
                <div className="admin-stat-skeleton" key={key} />
              ))
            : metrics.map((metric) => (
                <StatsCard key={metric.title} {...metric} />
              ))}
        </div>
        <div className="admin-primary-grid">
          <ChartCard
            title="Revenue Analytics"
            series={data?.trend || []}
            period={period}
            onPeriodChange={setPeriod}
            loading={loading}
          />
          <OrderStatusPanel data={data} loading={loading} />
        </div>
        <div className="admin-secondary-grid">
          <DataTable
            orders={data?.recentOrders || []}
            loading={loading}
            error={error}
          />
          <TopProducts products={data?.topProducts || []} loading={loading} />
        </div>
        {!loading && !error && !data && (
          <div className="admin-inline-empty">
            Dashboard analytics are currently unavailable.
          </div>
        )}
      </motion.div>
    </AdminLayout>
  );
}
