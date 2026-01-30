import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, SearchResult } from './services/api.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  title = 'ClipSearch';
  activeTab = 'search';

  // Upload State
  selectedFile: File | null = null;
  tags: string = '';
  uploading = false;
  uploadStatus = '';

  // Search State
  searchQuery = '';
  searchResults: SearchResult = { items: [], total: 0, page: 0, size: 10 };
  searching = false;
  currentPage = 0;
  pageSize = 10;

  // Filters
  filterContentType = '';
  filterTag = '';

  // Recent Uploads State
  recentUploads: any[] = [];
  loadingRecent = false;

  constructor(private api: ApiService) {}

  ngOnInit() {
    this.loadRecent();
  }

  setTab(tab: string) {
    this.activeTab = tab;
    if (tab === 'recent') {
      this.loadRecent();
    }
  }

  onFileSelected(event: any) {
    this.selectedFile = event.target.files[0];
  }

  upload() {
    if (!this.selectedFile) return;
    this.uploading = true;
    this.uploadStatus = 'Uploading...';
    this.api.upload(this.selectedFile, this.tags).subscribe({
      next: (res) => {
        this.uploadStatus = `Success! Upload ID: ${res.uploadId}`;
        this.uploading = false;
        this.selectedFile = null;
        this.tags = '';
        this.loadRecent();
      },
      error: (err) => {
        this.uploadStatus = `Error: ${err.message}`;
        this.uploading = false;
      }
    });
  }

  search(page: number = 0) {
    this.searching = true;
    this.currentPage = page;
    this.api.search(this.searchQuery, this.filterContentType, this.filterTag, this.currentPage, this.pageSize).subscribe({
      next: (res) => {
        this.searchResults = res;
        this.searchResults.page = this.currentPage;
        this.searching = false;
      },
      error: (err) => {
        console.error(err);
        this.searching = false;
      }
    });
  }

  clearFilters() {
    this.filterContentType = '';
    this.filterTag = '';
    this.search(0);
  }

  nextPage() {
    if ((this.currentPage + 1) * this.pageSize < this.searchResults.total) {
      this.search(this.currentPage + 1);
    }
  }

  prevPage() {
    if (this.currentPage > 0) {
      this.search(this.currentPage - 1);
    }
  }

  getTotalPages() {
    return Math.ceil(this.searchResults.total / this.pageSize);
  }

  downloadFile(id: string) {
    this.api.getDownloadUrl(id).subscribe({
      next: (res) => {
        window.open(res.url, '_blank');
      },
      error: (err) => {
        alert('Error getting download URL: ' + err.message);
      }
    });
  }

  loadRecent() {
    this.loadingRecent = true;
    this.api.listUploads(10).subscribe({
      next: (res) => {
        this.recentUploads = res.items;
        this.loadingRecent = false;
      },
      error: (err) => {
        console.error(err);
        this.loadingRecent = false;
      }
    });
  }

  formatSize(bytes: number) {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }
}