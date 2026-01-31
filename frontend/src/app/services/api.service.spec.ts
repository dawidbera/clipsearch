import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ApiService } from './api.service';

describe('ApiService', () => {
  let service: ApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ApiService]
    });
    service = TestBed.inject(ApiService);
    httpMock = TestBed.inject(HttpTestingController);
    
    // Handle initial config load
    const configReq = httpMock.expectOne('/config.json');
    configReq.flush({ apiUrl: '/api' });
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should call search API', () => {
    const mockResponse = { items: [], total: 0, page: 0, size: 10 };

    service.search('test').subscribe(res => {
      expect(res).toEqual(mockResponse);
    });

    const req = httpMock.expectOne(request => 
      request.url === '/api/search' && request.params.get('q') === 'test'
    );
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);
  });

  it('should upload a file', () => {
    const mockFile = new File([''], 'test.txt', { type: 'text/plain' });
    const mockResponse = { uploadId: '1', filename: 'test.txt' } as any;

    service.upload(mockFile, 'tag1').subscribe(res => {
      expect(res.uploadId).toBe('1');
    });

    const req = httpMock.expectOne('/api/uploads');
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);
  });
});
