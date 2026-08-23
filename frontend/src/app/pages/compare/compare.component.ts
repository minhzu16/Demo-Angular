import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { CompareService } from '../../services/compare.service';

@Component({
  selector: 'app-compare',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule],
  templateUrl: './compare.component.html',
  styleUrls: ['./compare.component.scss']
})
export class CompareComponent implements OnInit {
  private compareService = inject(CompareService);
  products: any[] = [];
  
  // Danh sách các thông số muốn so sánh
  specKeys = ['Brand', 'Model', 'Weight', 'Dimensions', 'Color', 'Material', 'Warranty'];

  ngOnInit() {
    this.compareService.products$.subscribe(data => {
      this.products = data;
    });
  }

  removeFromCompare(id: number) {
    this.compareService.removeFromCompare(id);
  }

  clearAll() {
    this.compareService.clearCompare();
  }

  getSpecValue(product: any, key: string): string {
    if (product.attributes && Array.isArray(product.attributes)) {
      const attr = product.attributes.find((a: any) => a.name.toLowerCase() === key.toLowerCase());
      return attr ? attr.value : '-';
    }
    return '-';
  }
}
