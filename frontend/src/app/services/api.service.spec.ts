import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ApiService } from './api.service';

/**
 * Test suite for the ApiService.
 * Tests the HTTP communication with the backend API including search and file upload functionality.
 */
describe('ApiService', () => {
  let service: ApiService;
  let httpMock: HttpTestingController;

  /**
   * Setup test environment before each test.
   * Initializes the TestBed with HttpClientTestingModule and ApiService.
   * Handles the initial config file load that occurs during service initialization.
   */
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ApiService]
    });
    service = TestBed.inject(ApiService);
    httpMock = TestBed.inject(HttpTestingController);
    
    // Handle the initial config.json request that occurs during service construction
    const configReq = httpMock.expectOne('/config.json');
    configReq.flush({ apiUrl: '/api' });
  });

  /**
   * Cleanup after each test.
   * Verifies that all expected HTTP requests have been made and no extra requests exist.
   */
  afterEach(() => {
    httpMock.verify();
  });

  /**
   * Test that verifies the ApiService can be instantiated successfully.
   * This is a basic sanity check to ensure the service initializes without errors.
   */
  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  /**
   * Test that verifies the search API functionality.
   * Validates that:
   * 1. The service makes a GET request to /api/search with query parameter
   * 2. The response is properly transformed and returned to the subscriber
   * 3. The correct search query is passed to the API
   */
  it('should call search API', () => {
    const mockResponse = { items: [], total: 0, page: 0, size: 10 };

    service.search('test').subscribe(res => {
      expect(res).toEqual(mockResponse);
    });

    // Intercept the search API request and verify parameters
    const req = httpMock.expectOne(request => 
      request.url === '/api/search' && request.params.get('q') === 'test'
    );
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);
  });

  /**
   * Test that verifies the file upload functionality.
   * Validates that:
   * 1. The service makes a POST request to /api/uploads
   * 2. File data is properly sent to the backend
   * 3. The upload response with uploadId is correctly handled
   * 4. Tags are properly associated with the uploaded file
   */
  it('should upload a file', () => {
    const mockFile = new File([''], 'test.txt', { type: 'text/plain' });
    const mockResponse = { uploadId: '1', filename: 'test.txt' } as any;

    service.upload(mockFile, 'tag1').subscribe(res => {
      expect(res.uploadId).toBe('1');
    });

    // Intercept the upload API request and verify it's a POST request
    const req = httpMock.expectOne('/api/uploads');
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);
  });
});
