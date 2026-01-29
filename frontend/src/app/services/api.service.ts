import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, forkJoin } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';

export interface UploadResponse {
  uploadId: string;
  bucket: string;
  key: string;
  filename: string;
  contentType: string;
  uploadedAt: string;
  tags: string[];
}

export interface SearchResult {
  items: any[];
  total: number;
  page: number;
  size: number;
}

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private apiUrl = '/api'; // Default

  constructor(private http: HttpClient) {
    this.loadConfig();
  }

  private loadConfig() {
    this.http.get<{apiUrl: string}>('/config.json').subscribe(config => {
      this.apiUrl = config.apiUrl;
    });
  }

  upload(file: File, tags: string): Observable<UploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    if (tags) {
      formData.append('tags', tags);
    }
    return this.http.post<UploadResponse>(`${this.apiUrl}/uploads`, formData);
  }

  search(q: string, contentType?: string, tag?: string, page: number = 0, size: number = 10): Observable<SearchResult> {
    let params = new HttpParams()
      .set('q', q || '')
      .set('page', page.toString())
      .set('size', size.toString());
    
    if (contentType) params = params.set('contentType', contentType);
    if (tag) params = params.set('tag', tag);

    return this.http.get<SearchResult>(`${this.apiUrl}/search`, { params });
  }

  listUploads(limit: number = 50): Observable<SearchResult> {
    return this.http.get<SearchResult>(`${this.apiUrl}/uploads`, {
      params: new HttpParams().set('limit', limit.toString())
    });
  }
}
