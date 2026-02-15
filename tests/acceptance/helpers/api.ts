import axios, { AxiosInstance, AxiosResponse } from 'axios';

/**
 * Base API client — barcha acceptance testlar uchun
 */
export class ApiClient {
  private authClient: AxiosInstance;
  private apiClient: AxiosInstance;
  private chatClient: AxiosInstance;
  private token: string | null = null;

  constructor() {
    this.authClient = axios.create({
      baseURL: process.env.AUTH_URL,
      timeout: 10000,
      headers: { 'Content-Type': 'application/json' },
    });

    this.apiClient = axios.create({
      baseURL: process.env.API_URL,
      timeout: 10000,
      headers: { 'Content-Type': 'application/json' },
    });

    this.chatClient = axios.create({
      baseURL: process.env.CHAT_URL,
      timeout: 10000,
      headers: { 'Content-Type': 'application/json' },
    });
  }

  setToken(token: string) {
    this.token = token;
    const authHeader = `Bearer ${token}`;
    this.authClient.defaults.headers.common['Authorization'] = authHeader;
    this.apiClient.defaults.headers.common['Authorization'] = authHeader;
    this.chatClient.defaults.headers.common['Authorization'] = authHeader;
  }

  clearToken() {
    this.token = null;
    delete this.authClient.defaults.headers.common['Authorization'];
    delete this.apiClient.defaults.headers.common['Authorization'];
    delete this.chatClient.defaults.headers.common['Authorization'];
  }

  getToken(): string | null {
    return this.token;
  }

  // ===== AUTH =====
  async sendOtp(phone: string): Promise<AxiosResponse> {
    return this.authClient.post('/api/auth/send-otp', { phone });
  }

  async verifyOtp(phone: string, code: string): Promise<AxiosResponse> {
    return this.authClient.post('/api/auth/verify-otp', { phone, code });
  }

  async refreshToken(refreshToken: string): Promise<AxiosResponse> {
    return this.authClient.post('/api/auth/refresh', { refreshToken });
  }

  async logout(): Promise<AxiosResponse> {
    return this.authClient.post('/api/auth/logout');
  }

  // ===== USER =====
  async getMyProfile(): Promise<AxiosResponse> {
    return this.apiClient.get('/api/users/me');
  }

  async updateProfile(data: Record<string, unknown>): Promise<AxiosResponse> {
    return this.apiClient.put('/api/users/me', data);
  }

  async getPublicProfile(userId: string): Promise<AxiosResponse> {
    return this.apiClient.get(`/api/users/${userId}`);
  }

  // ===== LISTINGS =====
  async createListing(data: Record<string, unknown>): Promise<AxiosResponse> {
    return this.apiClient.post('/api/listings', data);
  }

  async getListings(params?: Record<string, unknown>): Promise<AxiosResponse> {
    return this.apiClient.get('/api/listings', { params });
  }

  async getListing(id: string): Promise<AxiosResponse> {
    return this.apiClient.get(`/api/listings/${id}`);
  }

  async updateListing(id: string, data: Record<string, unknown>): Promise<AxiosResponse> {
    return this.apiClient.put(`/api/listings/${id}`, data);
  }

  async deleteListing(id: string): Promise<AxiosResponse> {
    return this.apiClient.delete(`/api/listings/${id}`);
  }

  async getMyListings(params?: Record<string, unknown>): Promise<AxiosResponse> {
    return this.apiClient.get('/api/listings/my', { params });
  }

  async searchListings(params?: Record<string, unknown>): Promise<AxiosResponse> {
    return this.apiClient.get('/api/listings/search', { params });
  }

  async getNearbyListings(params?: Record<string, unknown>): Promise<AxiosResponse> {
    return this.apiClient.get('/api/listings/nearby', { params });
  }

  // ===== FAVORITES =====
  async toggleFavorite(listingId: string): Promise<AxiosResponse> {
    return this.apiClient.post(`/api/favorites/${listingId}/toggle`);
  }

  async getFavorites(params?: Record<string, unknown>): Promise<AxiosResponse> {
    return this.apiClient.get('/api/favorites', { params });
  }

  async checkFavorite(listingId: string): Promise<AxiosResponse> {
    return this.apiClient.get(`/api/favorites/${listingId}/check`);
  }

  async getFavoriteIds(): Promise<AxiosResponse> {
    return this.apiClient.get('/api/favorites/ids');
  }

  // ===== CHAT =====
  async createChat(data: Record<string, unknown>): Promise<AxiosResponse> {
    return this.chatClient.post('/api/chats', data);
  }

  async getChats(params?: Record<string, unknown>): Promise<AxiosResponse> {
    return this.chatClient.get('/api/chats', { params });
  }

  async getMessages(roomId: string, params?: Record<string, unknown>): Promise<AxiosResponse> {
    return this.chatClient.get(`/api/chats/${roomId}/messages`, { params });
  }

  async sendMessage(roomId: string, data: Record<string, unknown>): Promise<AxiosResponse> {
    return this.chatClient.post(`/api/chats/${roomId}/messages`, data);
  }

  async markAsRead(roomId: string): Promise<AxiosResponse> {
    return this.chatClient.put(`/api/chats/${roomId}/read`);
  }

  // ===== REPORTS =====
  async createReport(data: Record<string, unknown>): Promise<AxiosResponse> {
    return this.apiClient.post('/api/reports', data);
  }

  async getReports(params?: Record<string, unknown>): Promise<AxiosResponse> {
    return this.apiClient.get('/api/reports', { params });
  }

  // ===== NOTIFICATIONS =====
  async getNotifications(params?: Record<string, unknown>): Promise<AxiosResponse> {
    return this.apiClient.get('/api/notifications', { params });
  }

  async getUnreadCount(): Promise<AxiosResponse> {
    return this.apiClient.get('/api/notifications/unread-count');
  }

  // ===== ADMIN =====
  async getPendingListings(params?: Record<string, unknown>): Promise<AxiosResponse> {
    return this.apiClient.get('/api/admin/listings', { params });
  }

  async approveListing(id: string): Promise<AxiosResponse> {
    return this.apiClient.put(`/api/admin/listings/${id}/approve`);
  }

  async rejectListing(id: string, data: Record<string, unknown>): Promise<AxiosResponse> {
    return this.apiClient.put(`/api/admin/listings/${id}/reject`, data);
  }

  async blockUser(userId: string, data: Record<string, unknown>): Promise<AxiosResponse> {
    return this.apiClient.put(`/api/admin/users/${userId}/block`, data);
  }

  // ===== HEALTH =====
  async healthCheck(): Promise<AxiosResponse> {
    return this.apiClient.get('/health');
  }
}

/**
 * Autentifikatsiya qilingan client yaratish uchun yordamchi
 */
export async function createAuthenticatedClient(
  phone?: string,
  otp?: string
): Promise<ApiClient> {
  const client = new ApiClient();
  const p = phone || process.env.TEST_PHONE!;
  const o = otp || process.env.TEST_OTP!;

  await client.sendOtp(p);
  const response = await client.verifyOtp(p, o);
  client.setToken(response.data.data.accessToken);
  return client;
}
