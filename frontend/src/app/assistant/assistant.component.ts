import { Component, DestroyRef, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { finalize, take } from 'rxjs/operators';

import { environment } from '../../environments/environment';
import { ChatResponse } from '../core/models';

interface Message {
  role: 'user' | 'assistant';
  content: string;
  sources?: { title: string; source: string; content: string; score: number }[];
}

@Component({
  selector: 'th-assistant',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <h1 class="page-title">Assistant IA</h1>
    <p class="muted">
      Vous interroge la base de connaissances des équipements (recherche vectorielle pgvector).
      Les réponses citent leurs sources — aucune génération libre.
    </p>

    <div class="chat card">
      <div class="bubbles">
        <div class="bubble empty" *ngIf="!messages().length">
          Posez une question sur les équipements, ex : « Quel est le seuil de température d'un compresseur ? »
        </div>
        <div *ngFor="let message of messages()" class="bubble" [class.user]="message.role === 'user'">
          <div class="text">{{ message.content }}</div>
          <div class="sources" *ngIf="message.sources?.length">
            <div *ngFor="let source of message.sources" class="source">
              <strong>{{ source.title }}</strong> ({{ source.source }} — score {{ source.score }})
              <div class="muted snippet">{{ source.content }}</div>
            </div>
          </div>
        </div>
      </div>
      <form (ngSubmit)="send()" class="input-row">
        <input [(ngModel)]="question" name="question" placeholder="Votre question…" autocomplete="off" />
        <button type="submit" [disabled]="busy() || !question.trim()">{{ busy() ? '…' : 'Envoyer' }}</button>
      </form>
    </div>
  `,
  styles: `
    .chat { max-width: 820px; display: flex; flex-direction: column; gap: 14px; }
    .bubbles { display: flex; flex-direction: column; gap: 10px; min-height: 300px; max-height: 60vh; overflow-y: auto; }
    .bubble { background: var(--th-surface-2); border-radius: 12px; padding: 12px 14px; max-width: 85%; }
    .bubble.user { align-self: flex-end; background: var(--th-primary); color: #fff; }
    .bubble.empty { background: none; color: var(--th-muted); text-align: center; margin: auto; }
    .sources { margin-top: 10px; display: flex; flex-direction: column; gap: 8px; }
    .source { background: var(--th-bg); border: 1px solid var(--th-border); border-radius: 8px; padding: 8px; font-size: 12px; }
    .snippet { margin-top: 4px; font-size: 11px; }
    .input-row { display: flex; gap: 8px; }
  `,
})
export class AssistantComponent {
  private http = inject(HttpClient);
  private destroyRef = inject(DestroyRef);

  messages = signal<Message[]>([]);
  question = '';
  busy = signal(false);

  send(): void {
    const question = this.question.trim();
    if (!question || this.busy()) {
      return;
    }
    this.messages.update((m) => [...m, { role: 'user', content: question }]);
    this.question = '';
    this.busy.set(true);

    this.http
      .post<ChatResponse>(`${environment.apiUrl}/v1/assistant/chat`, {
        question,
        tenantId: '11111111-1111-1111-1111-111111111111',
      })
      .pipe(
        take(1),
        finalize(() => this.busy.set(false)),
      )
      .subscribe({
        next: (response) =>
          this.messages.update((m) => [
            ...m,
            { role: 'assistant', content: response.answer, sources: response.sources },
          ]),
        error: () =>
          this.messages.update((m) => [
            ...m,
            { role: 'assistant', content: 'Service indisponible. Réessayez dans un instant.' },
          ]),
      });
  }
}