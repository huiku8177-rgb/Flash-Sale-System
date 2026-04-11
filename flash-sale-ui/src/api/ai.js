import { gatewayClient } from "./http";

export async function chatWithAssistant(payload) {
  const response = await gatewayClient.post("/ai/chat", payload);
  return response.data.data;
}

export async function getChatSession(sessionId) {
  const response = await gatewayClient.get(`/ai/chat/sessions/${sessionId}`);
  return response.data.data;
}

export async function listChatSessions(limit = 20) {
  const response = await gatewayClient.get("/ai/chat/sessions", {
    params: { limit }
  });
  return response.data.data;
}

export async function deleteChatSession(sessionId) {
  const response = await gatewayClient.delete(`/ai/chat/sessions/${sessionId}`);
  return response.data;
}

export async function resolveProductQuestion(question, maxCandidates = 6) {
  const response = await gatewayClient.post("/ai/chat/resolve-product", {
    question,
    maxCandidates
  });
  return response.data.data;
}
