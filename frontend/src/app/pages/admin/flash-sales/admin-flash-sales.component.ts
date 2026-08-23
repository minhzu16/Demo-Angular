import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ProductService, Product } from '../../../services/product.service';
import { ToastrService } from 'ngx-toastr';

@Component({
    selector: 'app-admin-flash-sales',
    standalone: true,
    imports: [CommonModule, FormsModule, ReactiveFormsModule],
    templateUrl: './admin-flash-sales.component.html',
    styleUrls: ['./admin-flash-sales.component.scss']
})
export class AdminFlashSalesComponent implements OnInit {
    private productService = inject(ProductService);
    private toastr = inject(ToastrService);
    private fb = inject(FormBuilder);

    flashSales: any[] = [];
    selectedSale: any = null;
    saleProducts: any[] = [];
    allProducts: Product[] = [];
    loading = false;
    showModal = false;
    modalMode: 'create' | 'edit' | 'products' = 'create';

    get activeCount() { return this.flashSales.filter(s => s.status === 'ACTIVE').length; }
    get scheduledCount() { return this.flashSales.filter(s => s.status === 'SCHEDULED').length; }
    get finishedCount() { return this.flashSales.filter(s => s.status === 'FINISHED').length; }

    saleForm = this.fb.group({
        name: ['', Validators.required],
        description: [''],
        startTime: ['', Validators.required],
        endTime: ['', Validators.required],
        status: ['SCHEDULED', Validators.required]
    });

    productForm = this.fb.group({
        productId: ['', Validators.required],
        salePrice: ['', [Validators.required, Validators.min(0)]],
        quantityLimit: ['', [Validators.required, Validators.min(1)]],
        maxPerUser: [1, [Validators.required, Validators.min(1)]]
    });

    ngOnInit() {
        this.loadFlashSales();
        this.loadAllProducts();
    }

    loadFlashSales() {
        this.loading = true;
        this.productService.getFlashSales().subscribe({
            next: (res) => {
                this.flashSales = res.flashSales || [];
                this.loading = false;
            },
            error: () => {
                this.toastr.error('Không thể tải danh sách Flash Sale');
                this.loading = false;
            }
        });
    }

    loadAllProducts() {
        this.productService.getProducts({ size: 100 }).subscribe({
            next: (res) => {
                this.allProducts = res.content || [];
            }
        });
    }

    openCreateModal() {
        this.modalMode = 'create';
        this.saleForm.reset({ status: 'SCHEDULED' });
        this.showModal = true;
    }

    openEditModal(sale: any) {
        this.modalMode = 'edit';
        this.selectedSale = sale;
        this.saleForm.patchValue({
            name: sale.name,
            description: sale.description,
            startTime: sale.startTime,
            endTime: sale.endTime,
            status: sale.status
        });
        this.showModal = true;
    }

    openProductsModal(sale: any) {
        this.modalMode = 'products';
        this.selectedSale = sale;
        this.loadSaleProducts(sale.id);
        this.showModal = true;
    }

    loadSaleProducts(saleId: number) {
        this.productService.getFlashSaleProducts(saleId).subscribe({
            next: (res) => {
                this.saleProducts = res.products || [];
            }
        });
    }

    closeModal() {
        this.showModal = false;
        this.selectedSale = null;
    }

    saveSale() {
        if (this.saleForm.invalid) return;

        const data = this.saleForm.value;
        if (this.modalMode === 'create') {
            this.productService.createFlashSale(data).subscribe({
                next: () => {
                    this.toastr.success('Tạo Flash Sale thành công');
                    this.loadFlashSales();
                    this.closeModal();
                },
                error: () => this.toastr.error('Lỗi khi tạo Flash Sale')
            });
        } else {
            this.productService.updateFlashSale(this.selectedSale.id, data).subscribe({
                next: () => {
                    this.toastr.success('Cập nhật Flash Sale thành công');
                    this.loadFlashSales();
                    this.closeModal();
                },
                error: () => this.toastr.error('Lỗi khi cập nhật Flash Sale')
            });
        }
    }

    deleteSale(id: number) {
        if (!confirm('Bạn có chắc muốn xóa Flash Sale này?')) return;
        this.productService.deleteFlashSale(id).subscribe({
            next: () => {
                this.toastr.success('Đã xóa Flash Sale');
                this.loadFlashSales();
            }
        });
    }

    endSale(id: number) {
        if (!confirm('Kết thúc đợt Flash Sale này ngay lập tức?')) return;
        this.productService.endFlashSale(id).subscribe({
            next: () => {
                this.toastr.success('Đã kết thúc Flash Sale');
                this.loadFlashSales();
            }
        });
    }

    addProduct() {
        if (this.productForm.invalid || !this.selectedSale) return;

        const product = this.allProducts.find(p => p.id === Number(this.productForm.value.productId));
        const data = {
            ...this.productForm.value,
            originalPrice: product?.price || 0
        };

        this.productService.addProductToFlashSale(this.selectedSale.id, data).subscribe({
            next: () => {
                this.toastr.success('Đã thêm sản phẩm vào Flash Sale');
                this.loadSaleProducts(this.selectedSale.id);
                this.productForm.reset({ maxPerUser: 1 });
            },
            error: () => this.toastr.error('Lỗi khi thêm sản phẩm')
        });
    }

    removeProduct(productId: number) {
        if (!this.selectedSale || !confirm('Xóa sản phẩm này khỏi Flash Sale?')) return;

        this.productService.removeProductFromFlashSale(this.selectedSale.id, productId).subscribe({
            next: () => {
                this.toastr.success('Đã xóa sản phẩm khỏi Flash Sale');
                this.loadSaleProducts(this.selectedSale.id);
            },
            error: () => this.toastr.error('Lỗi khi xóa sản phẩm')
        });
    }

    getStatusClass(status: string): string {
        switch (status) {
            case 'ACTIVE': return 'badge bg-success';
            case 'SCHEDULED': return 'badge bg-primary';
            case 'FINISHED': return 'badge bg-secondary';
            default: return 'badge bg-light text-dark';
        }
    }

    getProductName(id: number): string {
        return this.allProducts.find(p => p.id === id)?.name || 'Unknown';
    }
}
