<script setup lang="ts">
import { onMounted, ref } from 'vue'

type Quote = {
  id: number
  quote: string
  author: string
}

const quote = ref<Quote | null>(null)
const loading = ref(false)
const error = ref('')

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

onMounted(loadQuote)
</script>

<template>
  <main class="page-shell">
    <section class="hero-card">
      <p class="eyebrow">Frontend + Backend Pattern</p>
      <h1>Vue frontend calling the Spring Boot backend.</h1>
      <p class="lede">
        During local development, Vite proxies <code>/api</code> requests to the backend on
        <code>localhost:8080</code>. The browser only talks to relative paths.
      </p>

      <div class="actions">
        <button class="primary-button" type="button" @click="loadQuote" :disabled="loading">
          {{ loading ? 'Loading quote...' : 'Load another quote' }}
        </button>
      </div>
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
          The frontend calls <code>/api/quotes/random</code>, not an absolute backend host.
        </p>
      </article>

      <article class="info-card">
        <p class="eyebrow">Development</p>
        <p class="info-text">
          Vite proxies that request to <code>http://localhost:8080</code>.
        </p>
      </article>

      <article class="info-card">
        <p class="eyebrow">Default stance</p>
        <p class="info-text">
          This starter keeps CORS out of the backend and treats local proxying as the default.
        </p>
      </article>
    </section>
  </main>
</template>
