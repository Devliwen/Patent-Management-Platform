<template>
  <div class="feature-card" @click="handleClick">
    <div class="feature-icon">
      <el-icon :size="48"><component :is="icon" /></el-icon>
    </div>
    <h4 class="feature-title">{{ title }}</h4>
    <p class="feature-description">{{ description }}</p>
  </div>
</template>

<script setup lang="ts">
import { defineProps, defineEmits } from 'vue'
import type { Component, PropType } from 'vue'

const props = defineProps({
  icon: {
    type: [String, Object] as PropType<string | Component>,
    required: true
  },
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    required: true
  },
  to: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['click'])

const handleClick = () => {
  emit('click', props.to)
}
</script>

<style scoped>
.feature-card {
  background-color: var(--bg-primary);
  border-radius: var(--border-radius-md);
  box-shadow: var(--shadow-sm);
  padding: var(--spacing-lg) var(--spacing-md);
  text-align: center;
  transition: all 0.3s;
  cursor: pointer;
}

.feature-card:hover {
  transform: translateY(-5px);
  box-shadow: var(--shadow-md);
}

.feature-icon {
  font-size: 48px;
  color: var(--primary-color);
  margin-bottom: var(--spacing-md);
}

.feature-title {
  font-size: var(--font-size-lg);
  font-weight: 600;
  margin-bottom: var(--spacing-sm);
  color: var(--text-primary);
}

.feature-description {
  color: var(--text-secondary);
  line-height: 1.6;
}
</style>