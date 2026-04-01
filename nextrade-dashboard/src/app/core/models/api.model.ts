export interface ApiResponse<T> {
  timestamp: string;
  status: number;
  message: string;
  data: T;
  errors?: { field: string; message: string }[];
  traceId?: string;
  path?: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  sort?: string;
}
