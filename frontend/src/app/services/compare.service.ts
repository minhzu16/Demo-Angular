import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { ToastrService } from 'ngx-toastr';

@Injectable({
  providedIn: 'root'
})
export class CompareService {
  private productsSubject = new BehaviorSubject<any[]>([]);
  public products$ = this.productsSubject.asObservable();
  private maxItems = 4;

  constructor(private toastr: ToastrService) {
    const saved = localStorage.getItem('compare_products');
    if (saved) {
      try {
        this.productsSubject.next(JSON.parse(saved));
      } catch (e) {}
    }
  }

  get products(): any[] {
    return this.productsSubject.value;
  }

  addToCompare(product: any) {
    const current = this.products;
    if (current.find(p => p.id === product.id)) {
      this.toastr.info('Sản phẩm đã có trong danh sách so sánh');
      return;
    }
    
    if (current.length >= this.maxItems) {
      this.toastr.warning(`Chỉ có thể so sánh tối đa ${this.maxItems} sản phẩm`);
      return;
    }

    const updated = [...current, product];
    this.save(updated);
    this.toastr.success('Đã thêm vào danh sách so sánh');
  }

  removeFromCompare(productId: number) {
    const updated = this.products.filter(p => p.id !== productId);
    this.save(updated);
  }

  clearCompare() {
    this.save([]);
  }

  private save(products: any[]) {
    this.productsSubject.next(products);
    localStorage.setItem('compare_products', JSON.stringify(products));
  }
}
