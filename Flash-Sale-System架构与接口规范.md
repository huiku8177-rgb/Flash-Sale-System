# Flash-Sale-System 后端统一返回规范（V1）

## 1. 统一响应结构

所有后端接口统一返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": "2026-03-11T17:30:00"
}
```

字段含义：

- `code`：业务状态码（不是 HTTP 状态码）
- `message`：响应说明（用于提示）
- `data`：返回数据体（可为 `null`）
- `timestamp`：服务端响应时间

---

## 2. 状态码约定

### 2.1 通用状态码

- `200`：成功
- `400`：请求参数错误
- `401`：未认证
- `403`：无权限
- `500`：系统异常

### 2.2 业务状态码（秒杀场景）

- `2001`：库存不足
- `2002`：重复秒杀
- `2003`：通用业务异常

---

## 3. Controller 编写约束

1. 所有 Controller 返回 `Result<T>`，禁止直接返回 `String` 或裸对象。
2. 成功响应统一使用：`Result.success(data)` 或 `Result.success()`。
3. 失败响应统一使用：`Result.error(ResultCode.XXX)`。

示例：

```java
@GetMapping("/test")
public Result<String> test() {
    return Result.success("seckill ok");
}
```

---

## 4. 异常处理建议（下一步）

建议增加全局异常处理器（`@RestControllerAdvice`）：

- 参数校验异常 -> `PARAM_ERROR`
- 业务异常 -> `BUSINESS_ERROR`
- 未知异常 -> `SERVER_ERROR`

这样可以避免每个接口手动 try/catch，保证错误返回风格一致。

---

## 5. 前后端联调约定

前端统一按 `code` 判断业务结果：

- `code == 200`：视为成功
- `code != 200`：视为失败，展示 `message`

HTTP 状态码主要保留给网关/基础设施，不作为前端业务判断主依据。
