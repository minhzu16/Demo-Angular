import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { WebSocketService } from './services/web-socket.service';
import { ToastrService } from 'ngx-toastr';
import { AuthService } from './services/auth.service';
import { TranslateService } from '@ngx-translate/core';

import { ChatComponent } from './components/chat/chat.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, ChatComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent implements OnInit {
  private wsService = inject(WebSocketService);
  private toastr = inject(ToastrService);
  private authService = inject(AuthService);
  private translate = inject(TranslateService);

  ngOnInit() {
    this.translate.setDefaultLang('vi');
    const browserLang = this.translate.getBrowserLang();
    this.translate.use(browserLang?.match(/en|vi/) ? browserLang : 'vi');

    if (this.authService.isLoggedIn()) {
      this.wsService.connect();
    }

    this.wsService.notifications$.subscribe(notification => {
      this.toastr.info(notification.message, notification.title, {
        timeOut: 5000,
        progressBar: true,
        closeButton: true
      });
    });
  }
}
