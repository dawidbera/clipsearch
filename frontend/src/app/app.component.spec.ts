import { TestBed } from '@angular/core/testing';
import { AppComponent } from './app.component';

/**
 * Test suite for the AppComponent.
 * Tests the main application component initialization and rendering.
 */
describe('AppComponent', () => {
  /**
   * Setup test environment before each test.
   * Configures the TestBed with the AppComponent and compiles it.
   */
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
    }).compileComponents();
  });

  /**
   * Test that verifies the AppComponent can be created successfully.
   * This is a basic sanity check to ensure the component instantiates without errors.
   */
  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  /**
   * Test that verifies the component has the correct title property.
   * Ensures that the title is set to 'frontend' as expected.
   */
  it(`should have the 'frontend' title`, () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app.title).toEqual('frontend');
  });

  /**
   * Test that verifies the component renders the title in the DOM.
   * Detects changes and checks that the h1 element contains the expected greeting text.
   * This validates the template rendering functionality.
   */
  it('should render title', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();  // Trigger initial component change detection
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Hello, frontend');
  });
});
