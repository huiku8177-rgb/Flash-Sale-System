export function formatCurrency(value) {
  if (value === null || value === undefined || value === "") {
    return "--";
  }

  const numberValue = Number(value);
  if (Number.isNaN(numberValue)) {
    return value;
  }

  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY"
  }).format(numberValue);
}

export function formatDateTime(value) {
  if (!value) {
    return "--";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false
  }).format(date);
}

export function getCountdownText(startTime, endTime, now = Date.now()) {
  const start = startTime ? new Date(startTime).getTime() : null;
  const end = endTime ? new Date(endTime).getTime() : null;

  if (start && now < start) {
    return `距开始 ${formatDuration(start - now)}`;
  }

  if (end && now < end) {
    return `距结束 ${formatDuration(end - now)}`;
  }

  if (end && now >= end) {
    return "活动已结束";
  }

  return "活动进行中";
}

export function getProductPhase(product, now = Date.now()) {
  const start = product.startTime ? new Date(product.startTime).getTime() : null;
  const end = product.endTime ? new Date(product.endTime).getTime() : null;

  if (product.status !== 1) {
    return "offline";
  }

  if (start && now < start) {
    return "upcoming";
  }

  if (end && now >= end) {
    return "ended";
  }

  return "running";
}

export function getProductPhaseLabel(product, now = Date.now()) {
  const mapping = {
    running: "抢购中",
    upcoming: "即将开始",
    ended: "已结束",
    offline: "已下架"
  };
  return mapping[getProductPhase(product, now)] || "未知";
}

export function getProductPhaseType(product, now = Date.now()) {
  const mapping = {
    running: "danger",
    upcoming: "warning",
    ended: "info",
    offline: "info"
  };
  return mapping[getProductPhase(product, now)] || "info";
}

export function getOrderStatusText(status) {
  const mapping = {
    0: "新建",
    1: "已支付",
    2: "已取消"
  };
  return mapping[status] || `未知状态(${status})`;
}

function formatDuration(ms) {
  const totalSeconds = Math.max(Math.floor(ms / 1000), 0);
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  return [hours, minutes, seconds]
    .map((part) => String(part).padStart(2, "0"))
    .join(":");
}
