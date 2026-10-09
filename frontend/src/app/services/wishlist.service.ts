import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { BehaviorSubject, Observable, map, tap } from 'rxjs';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

export interface WishlistItem {
    id: number;
    productId: number;
    variantId?: number | null;
    notes?: string | null;
    addedAt?: string;
}

interface WishlistPage {
    content: WishlistItem[];
    totalElements?: number;
}

/**
 * Server-backed wishlist (GET/POST/DELETE /api/v1/wishlist).
 * Keeps the set of wished product ids in memory so product cards can render
 * their heart state without a request per card.
 */
@Injectable({ providedIn: 'root' })
export class WishlistService {
    private http = inject(HttpClient);
    private auth = inject(AuthService);
    private url = `${environment.apiBaseUrl}/wishlist`;

    private idsSubject = new BehaviorSubject<ReadonlySet<number>>(new Set<number>());
    readonly wishedIds$ = this.idsSubject.asObservable();
    private loaded = false;

    get count(): number {
        return this.idsSubject.value.size;
    }

    has(productId: number): boolean {
        return this.idsSubject.value.has(productId);
    }

    /** Fetch the wishlist and refresh the in-memory id set. */
    list(page = 0, size = 100): Observable<WishlistItem[]> {
        const params = new HttpParams().set('page', page).set('size', size);
        return this.http.get<WishlistPage>(this.url, { params }).pipe(
            map(res => res?.content ?? []),
            tap(items => this.idsSubject.next(new Set(items.map(i => Number(i.productId)))))
        );
    }

    /** Load once per session for signed-in users so hearts render correctly. */
    ensureLoaded(): void {
        if (this.loaded || !this.auth.isAuthenticated()) return;
        this.loaded = true;
        this.list().subscribe({ error: () => { this.loaded = false; } });
    }

    /** Adds or removes the product; emits the new wished state. */
    toggle(productId: number): Observable<boolean> {
        const wasWished = this.has(productId);
        const request$ = wasWished
            ? this.http.delete<unknown>(`${this.url}/${productId}`)
            : this.http.post<unknown>(this.url, { productId });

        return request$.pipe(
            map(() => !wasWished),
            tap(nowWished => {
                const next = new Set(this.idsSubject.value);
                nowWished ? next.add(productId) : next.delete(productId);
                this.idsSubject.next(next);
            })
        );
    }

    remove(productId: number): Observable<unknown> {
        return this.http.delete<unknown>(`${this.url}/${productId}`).pipe(
            tap(() => {
                const next = new Set(this.idsSubject.value);
                next.delete(productId);
                this.idsSubject.next(next);
            })
        );
    }
}
