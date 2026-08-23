import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Review {
    id?: number;
    productId: number;
    userId: number;
    userName: string;
    rating: number; // 1-5
    comment: string;
    mediaUrls?: string[];
    createdAt?: string;
    updatedAt?: string;
}

export interface ReviewListResponse {
    content: Review[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
}

@Injectable({ providedIn: 'root' })
export class ReviewService {
    private http = inject(HttpClient);
    private apiUrl = `${environment.apiBaseUrl}/reviews`;

    /**
     * Get reviews for a product with pagination
     */
    getProductReviews(productId: number, page: number = 0, size: number = 10): Observable<ReviewListResponse> {
        const params = new HttpParams()
            .set('page', page.toString())
            .set('size', size.toString());
        return this.http.get<ReviewListResponse>(`${this.apiUrl}/product/${productId}`, { params });
    }

    /**
     * Get reviews by user
     */
    getUserReviews(userId: number): Observable<Review[]> {
        return this.http.get<Review[]>(`${this.apiUrl}/user/${userId}`);
    }

    /**
     * Create a new review
     */
    createReview(review: Review): Observable<Review> {
        return this.http.post<Review>(`${this.apiUrl}/create`, review);
    }

    /**
     * Delete a review
     */
    deleteReview(id: number): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/${id}`);
    }
}
