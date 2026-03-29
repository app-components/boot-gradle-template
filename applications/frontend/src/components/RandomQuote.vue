<script setup lang="ts">
import { onMounted, ref } from 'vue'

type Quote = {
  id: number
  quote: string
  author: string
}

type QuoteEmailResponse = {
  quoteId: number
  quote: string
  author: string
  recipient: string
}

const quote = ref<Quote | null>(null)
const loading = ref(false)
const error = ref('')
const email = ref('developer@example.local')
const sending = ref(false)
const sendError = ref('')
const sendSuccess = ref('')

async function loadQuote() {
  loading.value = true
  error.value = ''

  try {
    const response = await fetch('/api/quotes/random')

    if (!response.ok) {
      throw new Error(`Backend returned ${response.status}`)
    }

    quote.value = await response.json()
  } catch (err) {
    quote.value = null
    error.value = err instanceof Error ? err.message : 'Unknown error'
  } finally {
    loading.value = false
  }
}

async function emailQuote() {
  sending.value = true
  sendError.value = ''
  sendSuccess.value = ''

  try {
    const response = await fetch('/api/quotes/email-random', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ email: email.value }),
    })

    if (!response.ok) {
      throw new Error(`Backend returned ${response.status}`)
    }

    const emailedQuote = (await response.json()) as QuoteEmailResponse
    quote.value = {
      id: emailedQuote.quoteId,
      quote: emailedQuote.quote,
      author: emailedQuote.author,
    }
    sendSuccess.value = `Sent a motivational quote to ${emailedQuote.recipient}. Check Mailpit on localhost:8025.`
  } catch (err) {
    sendError.value = err instanceof Error ? err.message : 'Unknown error'
  } finally {
    sending.value = false
  }
}

onMounted(loadQuote)
</script>

<template>
  <main class="page-shell">
    <section class="hero-card">
      <p class="eyebrow">Frontend + Backend Pattern</p>
      <h1>Vue frontend calling the Spring Boot backend.</h1>
      <p class="lede">
        During local development, Traefik routes browser traffic to the Vite dev server and proxies
        <code>/api</code> requests to the backend on <code>localhost:8080</code>. The browser only
        talks to relative paths.
      </p>

      <div class="actions">
        <button class="primary-button" type="button" @click="loadQuote" :disabled="loading">
          {{ loading ? 'Loading quote...' : 'Load another quote' }}
        </button>
      </div>

      <form class="email-form" @submit.prevent="emailQuote">
        <label class="email-label" for="quote-email">Email me a motivational quote</label>
        <div class="email-row">
          <input
            id="quote-email"
            v-model="email"
            class="email-input"
            type="email"
            name="email"
            autocomplete="email"
            required
          />
          <button class="primary-button" type="submit" :disabled="sending">
            {{ sending ? 'Sending...' : 'Send email' }}
          </button>
        </div>
        <p v-if="sendSuccess" class="form-feedback success-text">{{ sendSuccess }}</p>
        <p v-else-if="sendError" class="form-feedback error-inline">{{ sendError }}</p>
      </form>
    </section>

    <section class="quote-card">
      <p class="eyebrow">Random Quote</p>

      <p v-if="loading" class="status-text">Fetching a quote from the backend...</p>

      <p v-else-if="error" class="status-text error-text">
        Could not load the quote. {{ error }}
      </p>

      <template v-else-if="quote">
        <blockquote class="quote-text">“{{ quote.quote }}”</blockquote>
        <p class="quote-author">{{ quote.author }}</p>
      </template>
    </section>

    <section class="pattern-grid">
      <article class="info-card">
        <p class="eyebrow">Backend URL</p>
        <p class="info-text">
          The frontend calls relative backend paths like <code>/api/quotes/random</code> and
          <code>/api/quotes/email-random</code>.
        </p>
      </article>

      <article class="info-card">
        <p class="eyebrow">Development</p>
        <p class="info-text">
          Traefik proxies that request to <code>http://localhost:8080</code>.
        </p>
      </article>

      <article class="info-card">
        <p class="eyebrow">Default stance</p>
        <p class="info-text">
          This starter keeps CORS out of the backend and uses Mailpit to demo local SMTP flows.
        </p>
      </article>
    </section>
  </main>
</template>
