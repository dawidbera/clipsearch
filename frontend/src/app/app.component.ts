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

  search() {
    this.searching = true;
    this.api.search(this.searchQuery).subscribe({
      next: (res) => {
        this.searchResults = res;
        this.searching = false;
      },
      error: (err) => {
        console.error(err);
        this.searching = false;
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