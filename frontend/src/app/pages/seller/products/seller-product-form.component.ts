import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ProductService, Product } from '../../../services/product.service';
import { ShopService } from '../../../services/shop.service';
import { CategoryService, Category } from '../../../services/category.service';
import { ToastrService } from 'ngx-toastr';

@Component({
    selector: 'app-seller-product-form',
    standalone: true,
    imports: [CommonModule, RouterModule, ReactiveFormsModule],
    templateUrl: './seller-product-form.component.html',
    styleUrls: ['./seller-product-form.component.scss']
})
export class SellerProductFormComponent implements OnInit {
    private fb = inject(FormBuilder);
    private productService = inject(ProductService);
    private shopService = inject(ShopService);
    private categoryService = inject(CategoryService);
    private route = inject(ActivatedRoute);
    private router = inject(Router);
    private toastr = inject(ToastrService);

    isEditMode = false;
    productId: number | null = null;
    loading = false;
    submitting = false;
    error = '';
    shopId: number | null = null;
    categories: Category[] = [];

    productForm = this.fb.group({
        name: ['', [Validators.required, Validators.minLength(5)]],
        description: ['', [Validators.required, Validators.minLength(20)]],
        price: [0, [Validators.required, Validators.min(1000)]],
        originalPrice: [0, [Validators.min(0)]],
        stock: [0, [Validators.required, Validators.min(0)]],
        imageUrl: ['', [Validators.required]], // Simple URL input for now
        categoryId: [null as number | null, Validators.required],
        brand: ['']
    });

    ngOnInit() {
        this.loadCategories();
        this.shopService.getMyShop().subscribe({
            next: (shop) => {
                this.shopId = shop.id;
            },
            error: () => {
                this.error = 'Không tìm thấy thông tin shop';
            }
        });

        const id = this.route.snapshot.paramMap.get('id');
        if (id) {
            this.isEditMode = true;
            this.productId = +id;
            this.loadProduct(this.productId);
        }
    }

    loadCategories() {
        this.categoryService.getAllCategories().subscribe({
            next: (categories) => {
                this.categories = categories;
            },
            error: () => {
                console.error('Failed to load categories');
            }
        });
    }

    loadProduct(id: number) {
        this.loading = true;
        this.productService.getProductDetail(id).subscribe({
            next: (product: any) => { // Use any to bypass strict type check for now if interface mismatch
                this.productForm.patchValue({
                    name: product.name,
                    description: product.description,
                    price: product.price,
                    originalPrice: product.originalPrice,
                    stock: product.stock,
                    imageUrl: product.imageUrl,
                    categoryId: product.categoryId, // Ensure backend returns categoryId
                    brand: product.brand
                });
                this.loading = false;
            },
            error: (err) => {
                this.error = 'Không thể tải thông tin sản phẩm';
                this.loading = false;
            }
        });
    }

    onSubmit() {
        if (this.productForm.invalid) {
            this.productForm.markAllAsTouched();
            return;
        }

        this.submitting = true;
        const formValue = this.productForm.value;

        // Prepare request body
        const productData: any = {
            ...formValue,
            shopId: this.shopId // Ideally backend handles this from token/context
        };

        if (this.isEditMode && this.productId) {
            this.productService.updateProduct(this.productId, productData).subscribe({
                next: () => {
                    this.submitting = false;
                    this.toastr.success('Cập nhật sản phẩm thành công');
                    this.router.navigate(['/seller/products']);
                },
                error: (err) => {
                    this.error = err.error?.message || 'Cập nhật thất bại';
                    this.submitting = false;
                    this.toastr.error(this.error);
                }
            });
        } else {
            this.productService.createProduct(productData).subscribe({
                next: () => {
                    this.submitting = false;
                    this.toastr.success('Tạo sản phẩm thành công');
                    this.router.navigate(['/seller/products']);
                },
                error: (err) => {
                    this.error = err.error?.message || 'Tạo sản phẩm thất bại';
                    this.submitting = false;
                    this.toastr.error(this.error);
                }
            });
        }
    }
}
