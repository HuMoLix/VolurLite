<template>
  <div class="admin-image-view">
    <!-- Header Section -->
    <div class="page-header">
      <h2 class="page-title">{{ t('admin.images.title') }}</h2>
      <p class="page-desc">{{ t('admin.images.desc') }}</p>
    </div>

    <!-- Statistics Ribbon -->
    <div class="stat-ribbon">
      <div class="stat-mini-card">
        <div class="stat-icon-wrapper primary">
          <n-icon size="18">
            <LayersOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.totalImages }}</div>
          <div class="stat-label">{{ t('admin.images.stats.total') }}</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper warning">
          <n-icon size="18">
            <CubeOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.challengeImages }}</div>
          <div class="stat-label">{{ t('admin.images.stats.challenge') }}</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper info">
          <n-icon size="18">
            <ServerOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.baseImages }}</div>
          <div class="stat-label">{{ t('admin.images.stats.base') }}</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper success">
          <n-icon size="18">
            <ArchiveOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.totalSizeFormatted }}</div>
          <div class="stat-label">{{ t('admin.images.stats.storage') }}</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper secondary">
          <n-icon size="18">
            <GitNetworkOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.totalNodesCovered }}</div>
          <div class="stat-label">{{ t('admin.images.stats.nodes') }}</div>
        </div>
      </div>
    </div>

    <!-- Main Table Card -->
    <n-card class="table-card" :bordered="false">
      <!-- Filter & Action Controls -->
      <div class="filter-header">
        <div class="filter-left">
          <n-input
            v-model:value="searchKeyword"
            :placeholder="t('admin.images.search.placeholder')"
            clearable
            style="width: 320px;"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <n-icon :component="SearchOutline" />
            </template>
          </n-input>

          <n-select
            v-model:value="filterType"
            :placeholder="t('admin.images.search.typeAll')"
            clearable
            :options="typeOptions"
            style="width: 190px;"
            @update:value="handleSearch"
          />

          <n-button type="primary" class="theme-primary-btn" @click="handleSearch">
            <template #icon>
              <n-icon :component="SearchOutline" />
            </template>
            {{ t('admin.images.search.refresh') }}
          </n-button>
        </div>

        <div class="filter-right">
          <!-- Scan & Sync Docker Images Button -->
          <n-popconfirm
            @positive-click="handleScanAndSync"
            positive-text="确认探测"
            negative-text="取消"
          >
            <template #trigger>
              <n-button secondary :loading="scanningImages">
                <template #icon>
                  <n-icon :component="SyncOutline" />
                </template>
                {{ t('admin.images.search.scanBtn') }}
              </n-button>
            </template>
            {{ t('admin.images.search.scanConfirm') }}
          </n-popconfirm>

          <n-popconfirm
            @positive-click="handleSyncAllImages"
            positive-text="确认同步"
            negative-text="取消"
          >
            <template #trigger>
              <n-button secondary :loading="syncingAll">
                <template #icon>
                  <n-icon :component="CloudUploadOutline" />
                </template>
                {{ t('admin.images.search.syncAllBtn') }}
              </n-button>
            </template>
            {{ t('admin.images.search.syncAllConfirm') }}
          </n-popconfirm>

          <n-button type="primary" class="theme-primary-btn" @click="openPullModal">
            <template #icon>
              <n-icon :component="DownloadOutline" />
            </template>
            {{ t('admin.images.search.pullBtn') }}
          </n-button>
        </div>
      </div>

      <!-- Images Data Table -->
      <n-data-table
        :columns="imageColumns"
        :data="imageList"
        :loading="loadingTable"
        :bordered="false"
        class="images-table"
      />

      <!-- Pagination -->
      <div class="pagination-bar" v-if="totalCount > 0">
        <n-pagination
          v-model:page="currentPage"
          v-model:page-size="pageSize"
          :item-count="totalCount"
          :page-sizes="[10, 20, 50]"
          show-size-picker
          show-quick-jumper
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </n-card>

    <!-- Pull Image Modal -->
    <n-modal
      v-model:show="showPullModal"
      preset="card"
      :title="t('admin.images.pullModal.title')"
      style="max-width: 580px; border-radius: 12px;"
      :bordered="false"
    >
      <p class="modal-subtext">{{ t('admin.images.pullModal.subtitle') }}</p>

      <n-alert
        v-if="isMultiNode"
        type="warning"
        style="margin-top: 14px; font-size: 12.5px;"
        :bordered="false"
      >
        {{ t('admin.images.pullModal.multiNodeRule') }}
      </n-alert>
      <n-alert
        v-else
        type="info"
        style="margin-top: 14px; font-size: 12.5px;"
        :bordered="false"
      >
        {{ t('admin.images.pullModal.singleNodeRule') }}
      </n-alert>

      <n-form
        ref="pullFormRef"
        :model="pullForm"
        :rules="pullRules"
        label-placement="top"
        style="margin-top: 16px;"
      >
        <n-form-item :label="t('admin.images.pullModal.imageName')" path="image">
          <n-input
            v-model:value="pullForm.image"
            :placeholder="t('admin.images.pullModal.imageNamePlaceholder')"
          />
        </n-form-item>

        <n-form-item :label="t('admin.images.pullModal.targetNode')" path="targetNodeIp">
          <n-select
            v-model:value="pullForm.targetNodeIp"
            :options="nodeSelectOptions"
            :placeholder="t('admin.images.pullModal.targetNodePlaceholder')"
          />
        </n-form-item>

        <n-form-item :label="t('admin.images.pullModal.imageType')" path="imageType">
          <n-select
            v-model:value="pullForm.imageType"
            :options="typeFormOptions"
          />
        </n-form-item>
      </n-form>

      <template #footer>
        <div class="modal-footer">
          <n-button quaternary @click="showPullModal = false">
            {{ t('admin.images.pullModal.cancel') }}
          </n-button>
          <n-button
            type="primary"
            class="theme-primary-btn"
            :loading="submittingPull"
            @click="handleSubmitPull"
          >
            {{ t('admin.images.pullModal.submit') }}
          </n-button>
        </div>
      </template>
    </n-modal>

    <!-- Node Transfer Modal -->
    <n-modal
      v-model:show="showTransferModal"
      preset="card"
      :title="t('admin.images.transferModal.title')"
      style="max-width: 580px; border-radius: 12px;"
      :bordered="false"
    >
      <p class="modal-subtext">{{ t('admin.images.transferModal.subtitle') }}</p>

      <n-alert type="info" style="margin-top: 14px; font-size: 12.5px;" :bordered="false">
        <div style="font-weight: 600; margin-bottom: 2px;">{{ selectedImage?.fullImageName }}</div>
        <div>{{ t('admin.images.transferModal.transferRule') }}</div>
      </n-alert>

      <n-form
        ref="transferFormRef"
        :model="transferForm"
        :rules="transferRules"
        label-placement="top"
        style="margin-top: 16px;"
      >
        <n-form-item :label="t('admin.images.transferModal.sourceNode')" path="sourceNodeIp">
          <n-select
            v-model:value="transferForm.sourceNodeIp"
            :options="transferSourceOptions"
            :placeholder="t('admin.images.transferModal.sourceNodePlaceholder')"
            @update:value="handleTransferSourceChange"
          />
        </n-form-item>

        <n-form-item :label="t('admin.images.transferModal.targetNode')" path="targetNodeIp">
          <n-select
            v-model:value="transferForm.targetNodeIp"
            :options="transferTargetOptions"
            :placeholder="t('admin.images.transferModal.targetNodePlaceholder')"
          />
        </n-form-item>
      </n-form>

      <template #footer>
        <div class="modal-footer">
          <n-button quaternary @click="showTransferModal = false">
            {{ t('admin.images.transferModal.cancel') }}
          </n-button>
          <n-button
            type="primary"
            class="theme-primary-btn"
            :loading="submittingTransfer"
            @click="handleSubmitTransfer"
          >
            {{ t('admin.images.transferModal.submit') }}
          </n-button>
        </div>
      </template>
    </n-modal>

    <!-- Backup Modal -->
    <n-modal
      v-model:show="showBackupModal"
      preset="card"
      :title="t('admin.images.backupModal.title')"
      style="max-width: 580px; border-radius: 12px;"
      :bordered="false"
    >
      <p class="modal-subtext">{{ t('admin.images.backupModal.subtitle') }}</p>

      <n-alert type="info" style="margin-top: 14px; font-size: 12.5px;" :bordered="false">
        <div style="font-weight: 600; margin-bottom: 2px;">{{ selectedImage?.fullImageName }}</div>
        <div>{{ backupForm.backupType === 'NODE' ? t('admin.images.backupModal.nodeNotice') : t('admin.images.backupModal.archiveNotice') }}</div>
      </n-alert>

      <n-form
        ref="backupFormRef"
        :model="backupForm"
        :rules="backupRules"
        label-placement="top"
        style="margin-top: 16px;"
      >
        <n-form-item :label="t('admin.images.backupModal.backupType')" path="backupType">
          <n-radio-group v-model:value="backupForm.backupType" name="backupTypeGroup">
            <n-space>
              <n-radio value="NODE">
                {{ t('admin.images.backupModal.backupTypeNode') }}
              </n-radio>
              <n-radio value="ARCHIVE">
                {{ t('admin.images.backupModal.backupTypeArchive') }}
              </n-radio>
            </n-space>
          </n-radio-group>
        </n-form-item>

        <n-form-item
          v-if="backupForm.backupType === 'NODE'"
          :label="t('admin.images.backupModal.targetNode')"
          path="targetNodeIp"
        >
          <n-select
            v-model:value="backupForm.targetNodeIp"
            :options="backupTargetOptions"
            :placeholder="t('admin.images.backupModal.targetNodePlaceholder')"
          />
        </n-form-item>
      </n-form>

      <template #footer>
        <div class="modal-footer">
          <n-button quaternary @click="showBackupModal = false">
            {{ t('admin.images.backupModal.cancel') }}
          </n-button>
          <n-button
            type="primary"
            class="theme-primary-btn"
            :loading="submittingBackup"
            @click="handleSubmitBackup"
          >
            {{ backupForm.backupType === 'ARCHIVE' ? '立即导出下载' : t('admin.images.backupModal.submit') }}
          </n-button>
        </div>
      </template>
    </n-modal>

    <!-- Edit Category Modal -->
    <n-modal
      v-model:show="showEditTypeModal"
      preset="card"
      :title="t('admin.images.editTypeModal.title')"
      style="max-width: 480px; border-radius: 12px;"
      :bordered="false"
    >
      <p class="modal-subtext">{{ t('admin.images.editTypeModal.subtitle') }}</p>

      <div style="margin: 14px 0 16px; font-family: 'Fira Code', monospace; font-size: 13px; color: #111;">
        {{ selectedImage?.fullImageName }}
      </div>

      <n-form
        ref="editTypeFormRef"
        :model="editTypeForm"
        :rules="editTypeRules"
        label-placement="top"
      >
        <n-form-item :label="t('admin.images.editTypeModal.newType')" path="imageType">
          <n-select
            v-model:value="editTypeForm.imageType"
            :options="typeFormOptions"
            :placeholder="t('admin.images.editTypeModal.newTypePlaceholder')"
          />
        </n-form-item>
      </n-form>

      <template #footer>
        <div class="modal-footer">
          <n-button quaternary @click="showEditTypeModal = false">
            {{ t('admin.images.editTypeModal.cancel') }}
          </n-button>
          <n-button
            type="primary"
            class="theme-primary-btn"
            :loading="submittingEditType"
            @click="handleSubmitEditType"
          >
            {{ t('admin.images.editTypeModal.submit') }}
          </n-button>
        </div>
      </template>
    </n-modal>

    <!-- Delete Image Modal -->
    <n-modal
      v-model:show="showDeleteModal"
      preset="card"
      :title="t('admin.images.deleteModal.title')"
      style="max-width: 580px; border-radius: 12px;"
      :bordered="false"
    >
      <p class="modal-subtext">{{ t('admin.images.deleteModal.subtitle') }}</p>

      <n-alert type="warning" style="margin-top: 14px; font-size: 12.5px;" :bordered="false">
        <div style="font-weight: 600; margin-bottom: 2px;">{{ selectedDeleteImage?.fullImageName }}</div>
        <div>
          {{ t('admin.images.deleteModal.multiNodeNotice', { count: selectedDeleteImage?.nodeIps?.length || 0 }) }}
        </div>
      </n-alert>

      <n-form label-placement="top" style="margin-top: 16px;">
        <n-form-item :label="t('admin.images.deleteModal.scopeLabel')">
          <n-radio-group v-model:value="deleteScope" name="deleteScopeGroup">
            <n-space vertical :size="12">
              <n-radio value="SPECIFIC">
                <span style="font-weight: 500;">{{ t('admin.images.deleteModal.scopeSpecific') }}</span>
                <span class="delete-scope-hint">
                  {{ t('admin.images.deleteModal.scopeSpecificDesc') }}
                </span>
              </n-radio>
              <n-radio value="ALL">
                <span style="font-weight: 500; color: #dc2626;">{{ t('admin.images.deleteModal.scopeAll') }}</span>
                <span class="delete-scope-hint">
                  {{ t('admin.images.deleteModal.scopeAllDesc') }}
                </span>
              </n-radio>
            </n-space>
          </n-radio-group>
        </n-form-item>

        <n-form-item
          v-if="deleteScope === 'SPECIFIC'"
          :label="t('admin.images.deleteModal.selectNodes')"
          :feedback="deleteNodeFeedback"
          :validation-status="deleteNodeValidationStatus"
        >
          <div class="delete-node-box">
            <div class="delete-node-actions">
              <span class="node-select-hint">
                {{ t('admin.images.deleteModal.selectedCount', { selected: selectedDeleteNodes.length, total: deleteNodeOptions.length }) }}
              </span>
              <div class="action-btns">
                <n-button size="tiny" text type="primary" @click="handleSelectAllDeleteNodes">
                  {{ t('admin.images.deleteModal.selectAll') }}
                </n-button>
                <n-divider vertical style="margin: 0 4px;" />
                <n-button size="tiny" text @click="handleClearDeleteNodes">
                  {{ t('admin.images.deleteModal.clearAll') }}
                </n-button>
              </div>
            </div>

            <div class="delete-node-list">
              <div
                v-for="node in deleteNodeOptions"
                :key="node.ip"
                class="delete-node-item"
                :class="{ selected: selectedDeleteNodes.includes(node.ip) }"
                @click="toggleDeleteNode(node.ip)"
              >
                <n-checkbox
                  :checked="selectedDeleteNodes.includes(node.ip)"
                  @update:checked="() => toggleDeleteNode(node.ip)"
                  @click.stop
                />
                <div class="delete-node-info">
                  <div class="delete-node-name">
                    <span>{{ node.nodeName }}</span>
                    <span class="delete-node-ip">({{ node.ip }})</span>
                    <n-tag size="tiny" :type="node.isMaster ? 'info' : 'success'" round>
                      {{ node.role }}
                    </n-tag>
                    <n-tag v-if="node.isOffline" size="tiny" type="error" round>
                      离线
                    </n-tag>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </n-form-item>

        <n-alert
          v-else
          type="error"
          :bordered="false"
          style="margin-top: 8px; font-size: 12.5px;"
        >
          {{ t('admin.images.deleteModal.dangerWarning') }}
        </n-alert>
      </n-form>

      <template #footer>
        <div class="modal-footer">
          <n-button quaternary @click="showDeleteModal = false">
            {{ t('admin.images.deleteModal.cancel') }}
          </n-button>
          <n-button
            type="error"
            :loading="submittingDelete"
            :disabled="deleteScope === 'SPECIFIC' && selectedDeleteNodes.length === 0"
            @click="handleSubmitDelete"
          >
            {{ t('admin.images.deleteModal.submit') }}
          </n-button>
        </div>
      </template>
    </n-modal>

    <!-- Docker Hub Style Image Details & Layers Drawer -->
    <n-drawer
      v-model:show="showDetailDrawer"
      :width="740"
      placement="right"
      style="max-width: 92vw;"
    >
      <n-drawer-content :title="detailImage?.fullImageName || 'Docker 镜像详情'" closable>
        <div v-if="loadingDetail" class="detail-loading-wrapper">
          <n-spin size="large" />
          <p class="loading-subtext">正在从底层 Docker 引擎提取分层 Layer 与 Inspect 元数据...</p>
        </div>

        <div v-else-if="detailImage" class="image-detail-content">
          <!-- Overview Header Banner -->
          <div class="detail-header-card">
            <div class="header-top">
              <div class="image-full-title">
                {{ detailImage.fullImageName }}
              </div>
              <n-tag size="small" type="primary" round>
                {{ detailImage.imageType }}
              </n-tag>
            </div>
            <div class="header-meta-row">
              <div class="meta-item">
                <span class="meta-label">摘要 ID:</span>
                <span class="mono-text truncate" style="max-width: 380px;">{{ detailImage.imageId || '-' }}</span>
              </div>
              <div class="meta-item">
                <span class="meta-label">总大小:</span>
                <span class="mono-text font-bold">{{ detailImage.sizeFormatted }}</span>
              </div>
            </div>
            <div class="header-meta-row">
              <div class="meta-item">
                <span class="meta-label">系统架构:</span>
                <n-tag size="tiny" :bordered="false">{{ detailImage.os }}/{{ detailImage.architecture }}</n-tag>
              </div>
              <div class="meta-item">
                <span class="meta-label">引擎版本:</span>
                <span class="mono-text">{{ detailImage.dockerVersion || 'Docker Engine' }}</span>
              </div>
              <div class="meta-item">
                <span class="meta-label">分层数:</span>
                <span class="mono-text">{{ detailImage.layers ? detailImage.layers.length : 0 }} Layers</span>
              </div>
            </div>
          </div>

          <!-- Tabs for Layers, Inspect, and Overview -->
          <n-tabs type="line" animated style="margin-top: 14px;">
            <!-- Tab 1: Docker Hub Style Layers Breakdown -->
            <n-tab-pane name="layers" :tab="t('admin.images.detailDrawer.layersTab')">
              <div class="layers-container">
                <div class="layers-summary-bar">
                  <span>{{ t('admin.images.detailDrawer.layersCount', { count: detailImage.layers ? detailImage.layers.length : 0 }) }}</span>
                  <span class="total-size-badge">{{ t('admin.images.detailDrawer.totalSize') }}: {{ detailImage.sizeFormatted }}</span>
                </div>

                <div v-if="!detailImage.layers || detailImage.layers.length === 0" class="empty-layers">
                  <n-empty :description="t('admin.images.detailDrawer.noLayers')" />
                </div>

                <div v-else class="layers-timeline">
                  <div
                    v-for="(layer, idx) in detailImage.layers"
                    :key="idx"
                    class="layer-item-card"
                  >
                    <div class="layer-header">
                      <div class="layer-left">
                        <span class="layer-badge">#{{ layer.layerIndex }}</span>
                        <n-tag
                          size="tiny"
                          :type="getInstructionTagType(layer.instruction)"
                          class="instruction-tag"
                        >
                          {{ getInstructionKeyword(layer.instruction) }}
                        </n-tag>
                        <span v-if="layer.layerId" class="layer-id-text truncate">
                          {{ layer.layerId.startsWith('sha256:') ? layer.layerId.substring(7, 19) : layer.layerId }}
                        </span>
                      </div>
                      <div class="layer-right">
                        <span class="layer-size mono-text">{{ layer.sizeFormatted }}</span>
                        <span v-if="layer.createdAt" class="layer-time">{{ layer.createdAt }}</span>
                      </div>
                    </div>

                    <div class="layer-command-box">
                      <code class="command-text">{{ layer.instruction || layer.createdBy }}</code>
                      <n-button
                        size="tiny"
                        quaternary
                        circle
                        class="copy-btn"
                        @click="copyToClipboard(layer.instruction || layer.createdBy)"
                      >
                        <template #icon>
                          <n-icon :size="12" :component="CopyOutline" />
                        </template>
                      </n-button>
                    </div>
                  </div>
                </div>
              </div>
            </n-tab-pane>

            <!-- Tab 2: Runtime Environment / Inspect -->
            <n-tab-pane name="inspect" :tab="t('admin.images.detailDrawer.inspectTab')">
              <div class="inspect-container">
                <div class="inspect-section">
                  <div class="section-title">{{ t('admin.images.detailDrawer.entrypoint') }}</div>
                  <div class="code-box">
                    <span v-if="detailImage.entrypoint && detailImage.entrypoint.length > 0">
                      {{ detailImage.entrypoint.join(' ') }}
                    </span>
                    <span v-else class="text-muted">（未指定 Entrypoint）</span>
                  </div>
                </div>

                <div class="inspect-section">
                  <div class="section-title">{{ t('admin.images.detailDrawer.cmd') }}</div>
                  <div class="code-box">
                    <span v-if="detailImage.cmd && detailImage.cmd.length > 0">
                      {{ detailImage.cmd.join(' ') }}
                    </span>
                    <span v-else class="text-muted">（未指定 Cmd）</span>
                  </div>
                </div>

                <div class="inspect-section">
                  <div class="section-title">{{ t('admin.images.detailDrawer.workingDir') }}</div>
                  <div class="code-box">
                    <span>{{ detailImage.workingDir || '/' }}</span>
                  </div>
                </div>

                <div class="inspect-section">
                  <div class="section-title">{{ t('admin.images.detailDrawer.user') }}</div>
                  <div class="code-box">
                    <span>{{ detailImage.user || 'root' }}</span>
                  </div>
                </div>

                <div class="inspect-section" v-if="detailImage.exposedPorts && detailImage.exposedPorts.length > 0">
                  <div class="section-title">{{ t('admin.images.detailDrawer.exposedPorts') }}</div>
                  <div class="tag-row">
                    <n-tag
                      v-for="port in detailImage.exposedPorts"
                      :key="port"
                      size="small"
                      type="info"
                    >
                      {{ port }}
                    </n-tag>
                  </div>
                </div>

                <div class="inspect-section" v-if="detailImage.env && detailImage.env.length > 0">
                  <div class="section-title">{{ t('admin.images.detailDrawer.envVars') }}</div>
                  <div class="env-list">
                    <div v-for="(envItem, eIdx) in detailImage.env" :key="eIdx" class="env-row">
                      <span class="mono-text">{{ envItem }}</span>
                    </div>
                  </div>
                </div>

                <div class="inspect-section" v-if="detailImage.labels && Object.keys(detailImage.labels).length > 0">
                  <div class="section-title">{{ t('admin.images.detailDrawer.labels') }}</div>
                  <div class="labels-list">
                    <div v-for="(val, k) in detailImage.labels" :key="k" class="label-row">
                      <span class="label-key">{{ k }}:</span>
                      <span class="label-val">{{ val }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </n-tab-pane>

            <!-- Tab 3: Basic Metadata -->
            <n-tab-pane name="basic" :tab="t('admin.images.detailDrawer.basicTab')">
              <div class="basic-meta-container">
                <div class="meta-table">
                  <div class="meta-row">
                    <span class="row-label">仓库名称 (Repository):</span>
                    <span class="row-value mono-text">{{ detailImage.repository }}</span>
                  </div>
                  <div class="meta-row">
                    <span class="row-label">标签版本 (Tag):</span>
                    <span class="row-value mono-text">{{ detailImage.tag }}</span>
                  </div>
                  <div class="meta-row">
                    <span class="row-label">完整镜像全名:</span>
                    <span class="row-value mono-text font-bold">{{ detailImage.fullImageName }}</span>
                  </div>
                  <div class="meta-row">
                    <span class="row-label">镜像摘要 ID:</span>
                    <span class="row-value mono-text">{{ detailImage.imageId }}</span>
                  </div>
                  <div class="meta-row">
                    <span class="row-label">镜像功能分类:</span>
                    <span class="row-value">
                      <n-tag size="small" type="primary">{{ detailImage.imageType }}</n-tag>
                    </span>
                  </div>
                  <div class="meta-row">
                    <span class="row-label">物理存储体积:</span>
                    <span class="row-value mono-text">{{ detailImage.sizeFormatted }} ({{ detailImage.sizeBytes }} 字节)</span>
                  </div>
                  <div class="meta-row">
                    <span class="row-label">部署节点列表:</span>
                    <div class="row-value tag-row">
                      <n-tag
                        v-for="ip in detailImage.nodeIps"
                        :key="ip"
                        size="small"
                        :type="detailImage.offlineNodeIps?.includes(ip) ? 'error' : 'success'"
                      >
                        {{ ip }} {{ detailImage.offlineNodeIps?.includes(ip) ? '(离线)' : '' }}
                      </n-tag>
                    </div>
                  </div>
                  <div class="meta-row">
                    <span class="row-label">入库创建时间:</span>
                    <span class="row-value">{{ detailImage.createdAt }}</span>
                  </div>
                  <div class="meta-row" v-if="detailImage.updatedAt">
                    <span class="row-label">最近更新时间:</span>
                    <span class="row-value">{{ detailImage.updatedAt }}</span>
                  </div>
                </div>
              </div>
            </n-tab-pane>
          </n-tabs>
        </div>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, h } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  NCard,
  NForm,
  NFormItem,
  NInput,
  NSelect,
  NButton,
  NIcon,
  NTag,
  NDataTable,
  NPagination,
  NModal,
  NAlert,
  NPopconfirm,
  NTooltip,
  NRadioGroup,
  NRadio,
  NCheckbox,
  NDivider,
  NSpace,
  NDrawer,
  NDrawerContent,
  NTabs,
  NTabPane,
  NSpin,
  NEmpty,
  useMessage,
  type DataTableColumns,
  type SelectOption,
  type FormInst,
  type FormRules
} from 'naive-ui'
import {
  LayersOutline,
  CubeOutline,
  ServerOutline,
  GitNetworkOutline,
  SearchOutline,
  DownloadOutline,
  CloudUploadOutline,
  CopyOutline,
  TrashOutline,
  SyncOutline,
  SwapHorizontalOutline,
  ShieldCheckmarkOutline,
  EyeOutline,
  CreateOutline,
  ArchiveOutline
} from '@vicons/ionicons5'
import {
  getAdminImageStatisticsApi,
  getAdminImageListApi,
  getAdminImageDetailApi,
  updateAdminImageTypeApi,
  scanAndSyncAdminImagesApi,
  pullAdminImageApi,
  transferAdminImageApi,
  backupAdminImageApi,
  exportAdminImageApi,
  deleteAdminImageApi,
  syncAllImagesToResourceNodesApi
} from '@/api/image'
import { getAdminClusterNodesApi } from '@/api/cluster'
import type {
  AdminClusterImageVO,
  AdminClusterImageDetailVO,
  AdminClusterImageStatisticsVO
} from '@/types/image'
import type { AdminClusterNodeVO } from '@/types/cluster'

const { t } = useI18n()
const message = useMessage()

// Statistics State
const statistics = reactive<AdminClusterImageStatisticsVO>({
  totalImages: 0,
  challengeImages: 0,
  baseImages: 0,
  totalSizeBytes: 0,
  totalSizeFormatted: '0 B',
  totalNodesCovered: 0,
  syncingImages: 0
})

// Search & Filter State
const searchKeyword = ref('')
const filterType = ref<string | null>(null)
const currentPage = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)
const loadingTable = ref(false)
const imageList = ref<AdminClusterImageVO[]>([])
const syncingAll = ref(false)
const scanningImages = ref(false)

// Cluster Nodes State for Target Node selection
const clusterNodes = ref<AdminClusterNodeVO[]>([])
const isMultiNode = computed(() => clusterNodes.value.length > 1)

// Selected Image for Action Modals
const selectedImage = ref<AdminClusterImageVO | null>(null)
const exportingImageId = ref<string | number | null>(null)

// Detail Drawer State
const showDetailDrawer = ref(false)
const loadingDetail = ref(false)
const detailImage = ref<AdminClusterImageDetailVO | null>(null)

// Pull Modal State
const showPullModal = ref(false)
const submittingPull = ref(false)
const pullFormRef = ref<FormInst | null>(null)
const pullForm = reactive({
  image: '',
  targetNodeIp: '',
  imageType: 'CHALLENGE'
})

// Transfer Modal State
const showTransferModal = ref(false)
const submittingTransfer = ref(false)
const transferFormRef = ref<FormInst | null>(null)
const transferForm = reactive({
  sourceNodeIp: '',
  targetNodeIp: ''
})

// Backup Modal State
const showBackupModal = ref(false)
const submittingBackup = ref(false)
const backupFormRef = ref<FormInst | null>(null)
const backupForm = reactive({
  backupType: 'NODE',
  targetNodeIp: ''
})

// Edit Category Modal State
const showEditTypeModal = ref(false)
const submittingEditType = ref(false)
const editTypeFormRef = ref<FormInst | null>(null)
const editTypeForm = reactive({
  imageType: 'CHALLENGE'
})

// Delete Modal State
const showDeleteModal = ref(false)
const submittingDelete = ref(false)
const selectedDeleteImage = ref<AdminClusterImageVO | null>(null)
const deleteScope = ref<'SPECIFIC' | 'ALL'>('SPECIFIC')
const selectedDeleteNodes = ref<string[]>([])

const deleteNodeOptions = computed(() => {
  if (!selectedDeleteImage.value?.nodeIps) return []
  return selectedDeleteImage.value.nodeIps.map((ip) => {
    const matchedNode = clusterNodes.value.find((n) => n.ipAddress === ip)
    const isMaster = matchedNode?.role === 'MASTER'
    const isOffline = matchedNode?.status === 'OFFLINE' || selectedDeleteImage.value?.offlineNodeIps?.includes(ip)
    return {
      ip,
      nodeName: matchedNode?.nodeName || (isMaster ? '主控制节点' : '工作节点'),
      role: isMaster ? '控制面主节点' : '资源工作节点',
      isMaster,
      isOffline,
      statusText: isOffline ? '离线 / 脱机' : '在线就绪',
      label: `${matchedNode?.nodeName || ip} (${ip})`
    }
  })
})

const deleteNodeFeedback = computed(() => {
  if (selectedDeleteNodes.value.length === 0) {
    return t('admin.images.deleteModal.selectNodesRequired')
  }
  if (
    selectedDeleteImage.value?.nodeIps &&
    selectedDeleteNodes.value.length === selectedDeleteImage.value.nodeIps.length
  ) {
    return t('admin.images.deleteModal.selectAllNodesWarning')
  }
  return ''
})

const deleteNodeValidationStatus = computed<'error' | 'warning' | undefined>(() => {
  if (selectedDeleteNodes.value.length === 0) {
    return 'error'
  }
  if (
    selectedDeleteImage.value?.nodeIps &&
    selectedDeleteNodes.value.length === selectedDeleteImage.value.nodeIps.length
  ) {
    return 'warning'
  }
  return undefined
})

const typeOptions = computed<SelectOption[]>(() => [
  { label: t('admin.images.search.typeAll'), value: null as any },
  { label: t('admin.images.search.typeChallenge'), value: 'CHALLENGE' },
  { label: t('admin.images.search.typeBase'), value: 'BASE' },
  { label: t('admin.images.search.typeSystem'), value: 'SYSTEM' }
])

const typeFormOptions = computed<SelectOption[]>(() => [
  { label: t('admin.images.search.typeChallenge'), value: 'CHALLENGE' },
  { label: t('admin.images.search.typeBase'), value: 'BASE' },
  { label: t('admin.images.search.typeSystem'), value: 'SYSTEM' }
])

// Target Node Options for Pull Modal
const nodeSelectOptions = computed<SelectOption[]>(() => {
  if (clusterNodes.value.length === 0) {
    return [{ label: '127.0.0.1 (本地主节点)', value: '127.0.0.1' }]
  }

  const multi = clusterNodes.value.length > 1
  const options: SelectOption[] = []

  if (multi) {
    options.push({
      label: t('admin.images.pullModal.allResourceNodes'),
      value: 'ALL_RESOURCE_NODES'
    })
  }

  clusterNodes.value.forEach((node) => {
    const isMaster = node.role === 'MASTER'
    const label = `${node.nodeName} (${node.ipAddress}) - ${
      isMaster ? (multi ? '控制面主节点 [受保护禁止下载]' : '主节点 [单机环境]') : '资源工作节点'
    }`

    options.push({
      label,
      value: node.ipAddress,
      disabled: multi && isMaster
    })
  })

  return options
})

// Transfer Source Node Options
const transferSourceOptions = computed<SelectOption[]>(() => {
  if (!selectedImage.value || !selectedImage.value.nodeIps || selectedImage.value.nodeIps.length === 0) {
    return []
  }
  return selectedImage.value.nodeIps.map((ip) => {
    const matchedNode = clusterNodes.value.find((n) => n.ipAddress === ip)
    const label = matchedNode
      ? `${matchedNode.nodeName} (${ip}) - ${matchedNode.role === 'MASTER' ? '控制面主节点' : '资源工作节点'}`
      : ip
    return { label, value: ip }
  })
})

// Transfer Target Node Options
const transferTargetOptions = computed<SelectOption[]>(() => {
  const multi = clusterNodes.value.length > 1
  const isChallenge = selectedImage.value?.imageType === 'CHALLENGE'

  return clusterNodes.value
    .filter((n) => n.ipAddress !== transferForm.sourceNodeIp)
    .map((node) => {
      const isMaster = node.role === 'MASTER'
      const disabled = multi && isMaster && isChallenge
      const label = `${node.nodeName} (${node.ipAddress}) - ${
        isMaster ? (multi ? '主控制面 [多节点保护]' : '单机主节点') : '资源工作节点'
      }`
      return { label, value: node.ipAddress, disabled }
    })
})

// Backup Target Node Options
const backupTargetOptions = computed<SelectOption[]>(() => {
  const deployedIps = new Set(selectedImage.value?.nodeIps || [])
  return clusterNodes.value.map((node) => {
    const isMaster = node.role === 'MASTER'
    const alreadyDeployed = deployedIps.has(node.ipAddress)
    const label = `${node.nodeName} (${node.ipAddress}) - ${
      isMaster ? '控制面主节点' : '资源工作节点'
    }${alreadyDeployed ? ' [已存在副本]' : ''}`
    return {
      label,
      value: node.ipAddress,
      disabled: alreadyDeployed
    }
  })
})

const pullRules: FormRules = {
  image: [
    { required: true, message: () => t('admin.images.pullModal.imageNameRequired'), trigger: 'blur' },
    { min: 3, max: 255, message: '镜像名称长度必须在3到255个字符之间', trigger: 'blur' }
  ],
  targetNodeIp: [
    { required: true, message: () => t('admin.images.pullModal.targetNodeRequired'), trigger: ['blur', 'change'] },
    {
      validator(_rule, value) {
        if (!value) return true
        if (isMultiNode.value) {
          const master = clusterNodes.value.find((n) => n.role === 'MASTER')
          if (master && master.ipAddress === value) {
            return new Error('多节点集群中禁止选择控制面主节点，请选择资源工作节点！')
          }
        }
        return true
      },
      trigger: 'change'
    }
  ]
}

const transferRules: FormRules = {
  sourceNodeIp: [
    { required: true, message: () => t('admin.images.transferModal.sourceNodeRequired'), trigger: ['blur', 'change'] }
  ],
  targetNodeIp: [
    { required: true, message: () => t('admin.images.transferModal.targetNodeRequired'), trigger: ['blur', 'change'] },
    {
      validator(_rule, value) {
        if (value && value === transferForm.sourceNodeIp) {
          return new Error(t('admin.images.transferModal.sameNodeError'))
        }
        return true
      },
      trigger: 'change'
    }
  ]
}

const backupRules: FormRules = {
  backupType: [
    { required: true, message: '请选择备份模式', trigger: 'change' }
  ],
  targetNodeIp: [
    {
      validator(_rule, value) {
        if (backupForm.backupType === 'NODE' && !value) {
          return new Error(t('admin.images.backupModal.targetNodeRequired'))
        }
        return true
      },
      trigger: ['blur', 'change']
    }
  ]
}

const editTypeRules: FormRules = {
  imageType: [
    { required: true, message: '请选择镜像分类属性', trigger: ['blur', 'change'] }
  ]
}

// Table Columns (without 关联赛题)
const imageColumns = computed<DataTableColumns<AdminClusterImageVO>>(() => [
  {
    title: t('admin.images.table.image'),
    key: 'fullImageName',
    width: 280,
    render(row) {
      return h('div', { class: 'image-id-cell' }, [
        h('span', { class: 'mono-text truncate' }, row.fullImageName),
        h(
          NButton,
          {
            size: 'tiny',
            quaternary: true,
            circle: true,
            onClick: () => copyToClipboard(row.fullImageName)
          },
          { default: () => h(NIcon, { size: 13, component: CopyOutline }) }
        )
      ])
    }
  },
  {
    title: t('admin.images.table.type'),
    key: 'imageType',
    width: 140,
    render(row) {
      const isChallenge = row.imageType === 'CHALLENGE'
      return h('div', { class: 'category-cell' }, [
        h(
          NTag,
          {
            size: 'small',
            type: isChallenge ? 'warning' : 'info',
            round: true,
            style: { cursor: 'pointer' },
            onClick: () => openEditTypeModal(row)
          },
          { default: () => (isChallenge ? '赛题镜像' : row.imageType === 'BASE' ? '基础镜像' : '系统镜像') }
        ),
        h(
          NButton,
          {
            size: 'tiny',
            quaternary: true,
            circle: true,
            onClick: () => openEditTypeModal(row)
          },
          { default: () => h(NIcon, { size: 12, component: CreateOutline }) }
        )
      ])
    }
  },
  {
    title: t('admin.images.table.size'),
    key: 'sizeFormatted',
    width: 110,
    render(row) {
      return h('span', { class: 'mono-text' }, row.sizeFormatted || '0 B')
    }
  },
  {
    title: t('admin.images.table.nodes'),
    key: 'nodeIps',
    width: 240,
    render(row) {
      if (!row.nodeIps || row.nodeIps.length === 0) {
        return h('span', { class: 'text-muted' }, '未分发')
      }

      return h(
        'div',
        { class: 'node-tag-list' },
        row.nodeIps.map((ip) => {
          const matched = clusterNodes.value.find((n) => n.ipAddress === ip)
          const isMaster = matched?.role === 'MASTER'
          const isOffline = matched?.status === 'OFFLINE' || row.offlineNodeIps?.includes(ip)
          const label = matched
            ? `${matched.nodeName} (${isMaster ? '主' : '从'})${isOffline ? ' (离线)' : ''}`
            : `${ip}${isOffline ? ' (离线)' : ''}`

          return h(
            NTooltip,
            { trigger: 'hover' },
            {
              trigger: () =>
                h(
                  NTag,
                  {
                    size: 'tiny',
                    round: true,
                    type: isOffline ? 'error' : isMaster ? 'info' : 'success',
                    key: ip,
                    style: { cursor: 'pointer' },
                    onClick: () => copyToClipboard(ip)
                  },
                  { default: () => label }
                ),
              default: () =>
                h('div', { class: 'node-tooltip-content' }, [
                  h('div', `节点名称: ${matched?.nodeName || '未知节点'}`),
                  h('div', `IP 地址: ${ip}`),
                  h('div', `角色类型: ${isMaster ? '控制面主节点' : '赛题资源工作节点'}`),
                  h('div', `容器引擎: ${matched?.dockerVersion || 'Docker Engine'}`),
                  h('div', `节点状态: ${isOffline ? 'OFFLINE (脱机离线)' : matched?.status || 'READY'}`),
                  h('div', { style: 'margin-top: 4px; color: #94a3b8; font-size: 11px;' }, '点击可快速复制 IP 地址')
                ])
            }
          )
        })
      )
    }
  },
  {
    title: t('admin.images.table.status'),
    key: 'status',
    width: 130,
    render(row) {
      const isOffline = row.status === 'OFFLINE' || row.isOffline
      const isPulled = row.status === 'PULLED'
      const isMigrating = row.status === 'MIGRATING'
      let type: 'success' | 'warning' | 'info' | 'error' = isOffline ? 'error' : isPulled ? 'success' : isMigrating ? 'warning' : 'info'
      let text = isOffline ? '离线 (节点脱机)' : isPulled ? '就绪' : isMigrating ? '迁移中' : '同步中'
      return h(
        NTag,
        { size: 'small', type, round: true },
        { default: () => text }
      )
    }
  },
  {
    title: t('admin.images.table.pullTime'),
    key: 'createdAt',
    width: 160,
    render(row) {
      return h('span', { class: 'time-text' }, row.createdAt)
    }
  },
  {
    title: t('admin.images.table.actions'),
    key: 'actions',
    width: 310,
    render(row) {
      return h('div', { class: 'table-action-btns' }, [
        // 查看分层详情按钮
        h(
          NButton,
          {
            size: 'tiny',
            quaternary: true,
            type: 'default',
            onClick: () => openDetailDrawer(row)
          },
          {
            icon: () => h(NIcon, { size: 13, component: EyeOutline }),
            default: () => t('admin.images.table.detail')
          }
        ),
        // 转移按钮
        h(
          NButton,
          {
            size: 'tiny',
            quaternary: true,
            type: 'primary',
            onClick: () => openTransferModal(row)
          },
          {
            icon: () => h(NIcon, { size: 13, component: SwapHorizontalOutline }),
            default: () => t('admin.images.table.transfer')
          }
        ),
        // 备份按钮
        h(
          NButton,
          {
            size: 'tiny',
            quaternary: true,
            type: 'info',
            onClick: () => openBackupModal(row)
          },
          {
            icon: () => h(NIcon, { size: 13, component: ShieldCheckmarkOutline }),
            default: () => t('admin.images.table.backup')
          }
        ),
        // 导出按钮
        h(
          NButton,
          {
            size: 'tiny',
            quaternary: true,
            type: 'default',
            loading: exportingImageId.value === row.id,
            disabled: exportingImageId.value !== null,
            onClick: () => handleExportImage(row)
          },
          {
            icon: () => h(NIcon, { size: 13, component: DownloadOutline }),
            default: () => t('admin.images.table.export')
          }
        ),
        // 删除按钮
        row.nodeIps && row.nodeIps.length > 1
          ? h(
              NButton,
              {
                size: 'tiny',
                quaternary: true,
                type: 'error',
                onClick: () => openDeleteModal(row)
              },
              {
                icon: () => h(NIcon, { size: 13, component: TrashOutline }),
                default: () => t('admin.images.table.delete')
              }
            )
          : h(
              NPopconfirm,
              {
                onPositiveClick: () => handleDeleteImage(row),
                positiveText: '确认删除',
                negativeText: '取消'
              },
              {
                trigger: () =>
                  h(
                    NButton,
                    { size: 'tiny', quaternary: true, type: 'error' },
                    {
                      icon: () => h(NIcon, { size: 13, component: TrashOutline }),
                      default: () => t('admin.images.table.delete')
                    }
                  ),
                default: () => {
                  const nodeIp = row.nodeIps?.[0]
                  const matchedNode = nodeIp ? clusterNodes.value.find((n) => n.ipAddress === nodeIp) : null
                  const nodeName = matchedNode ? `${matchedNode.nodeName} (${nodeIp})` : (nodeIp || '当前节点')
                  return `确定要从节点 [${nodeName}] 彻底删除该镜像吗？相关的数据库元数据也将被全量清除。`
                }
              }
            )
      ])
    }
  }
])

function getInstructionKeyword(inst?: string): string {
  if (!inst) return 'CMD'
  const firstWord = inst.trim().split(' ')[0]
  return firstWord.toUpperCase()
}

function getInstructionTagType(inst?: string): 'info' | 'success' | 'warning' | 'error' | 'default' {
  const kw = getInstructionKeyword(inst)
  switch (kw) {
    case 'RUN':
      return 'info'
    case 'COPY':
    case 'ADD':
      return 'warning'
    case 'CMD':
    case 'ENTRYPOINT':
      return 'success'
    case 'ENV':
    case 'ARG':
      return 'default'
    case 'EXPOSE':
      return 'info'
    default:
      return 'default'
  }
}

async function fetchStatistics() {
  try {
    const res = await getAdminImageStatisticsApi()
    if (res.data) {
      Object.assign(statistics, res.data)
    }
  } catch (err) {
    console.warn('Failed to load image statistics:', err)
  }
}

async function fetchClusterNodes() {
  try {
    const res = await getAdminClusterNodesApi()
    if (res.data) {
      clusterNodes.value = res.data
    }
  } catch (err) {
    console.warn('Failed to load cluster nodes:', err)
  }
}

async function fetchImages() {
  loadingTable.value = true
  try {
    const res = await getAdminImageListApi({
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value || undefined,
      imageType: filterType.value || undefined
    })
    if (res.data) {
      imageList.value = res.data.list
      totalCount.value = res.data.total
    }
  } catch (err) {
    console.warn('Failed to load images:', err)
  } finally {
    loadingTable.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  fetchImages()
  fetchClusterNodes()
  fetchStatistics()
}

function handlePageChange(page: number) {
  currentPage.value = page
  fetchImages()
}

function handlePageSizeChange(size: number) {
  pageSize.value = size
  currentPage.value = 1
  fetchImages()
}

async function handleScanAndSync() {
  scanningImages.value = true
  try {
    await scanAndSyncAdminImagesApi()
    message.success('已完成 Docker 探测与 MySQL 孤儿记录清理对账！')
    await fetchImages()
    await fetchStatistics()
    await fetchClusterNodes()
  } catch {
    message.error('扫描 Docker 集群镜像失败')
  } finally {
    scanningImages.value = false
  }
}

async function openDetailDrawer(image: AdminClusterImageVO) {
  detailImage.value = null
  showDetailDrawer.value = true
  loadingDetail.value = true
  try {
    const res = await getAdminImageDetailApi(image.id)
    if (res.data) {
      detailImage.value = res.data
    }
  } catch {
    message.error('获取镜像深度分层详情失败')
  } finally {
    loadingDetail.value = false
  }
}

function openEditTypeModal(image: AdminClusterImageVO) {
  selectedImage.value = image
  editTypeForm.imageType = image.imageType || 'CHALLENGE'
  showEditTypeModal.value = true
}

async function handleSubmitEditType() {
  if (!editTypeFormRef.value || !selectedImage.value) return
  await editTypeFormRef.value.validate()

  submittingEditType.value = true
  try {
    await updateAdminImageTypeApi(selectedImage.value.id, {
      imageType: editTypeForm.imageType
    })
    message.success(t('admin.images.editTypeModal.success'))
    showEditTypeModal.value = false
    await fetchImages()
    await fetchStatistics()
  } catch {
    // handled by axios interceptor
  } finally {
    submittingEditType.value = false
  }
}

async function openPullModal() {
  await fetchClusterNodes()
  pullForm.image = ''
  pullForm.imageType = 'CHALLENGE'

  if (isMultiNode.value) {
    pullForm.targetNodeIp = 'ALL_RESOURCE_NODES'
  } else {
    pullForm.targetNodeIp = clusterNodes.value[0]?.ipAddress || '127.0.0.1'
  }

  showPullModal.value = true
}

async function handleSubmitPull() {
  if (!pullFormRef.value) return
  await pullFormRef.value.validate()

  submittingPull.value = true
  try {
    await pullAdminImageApi(pullForm)
    message.success(t('admin.images.pullModal.pullSuccess'))
    showPullModal.value = false
    await fetchImages()
    await fetchStatistics()
  } catch {
    // handled by axios interceptor
  } finally {
    submittingPull.value = false
  }
}

async function openTransferModal(image: AdminClusterImageVO) {
  selectedImage.value = image
  await fetchClusterNodes()

  transferForm.sourceNodeIp = image.nodeIps?.[0] || ''
  transferForm.targetNodeIp = ''

  showTransferModal.value = true
}

function handleTransferSourceChange() {
  transferForm.targetNodeIp = ''
}

async function handleSubmitTransfer() {
  if (!transferFormRef.value || !selectedImage.value) return
  await transferFormRef.value.validate()

  submittingTransfer.value = true
  try {
    await transferAdminImageApi({
      imageId: selectedImage.value.id,
      sourceNodeIp: transferForm.sourceNodeIp,
      targetNodeIp: transferForm.targetNodeIp
    })
    message.info('镜像节点转移任务已提交后台异步执行，可前往【任务管理】实时追踪进度')
    showTransferModal.value = false
    await fetchImages()
    await fetchStatistics()
    await fetchClusterNodes()
  } catch {
    // handled by axios interceptor
  } finally {
    submittingTransfer.value = false
  }
}

async function openBackupModal(image: AdminClusterImageVO) {
  selectedImage.value = image
  await fetchClusterNodes()

  backupForm.backupType = 'NODE'
  backupForm.targetNodeIp = ''

  showBackupModal.value = true
}

async function handleExportImage(image: AdminClusterImageVO) {
  if (exportingImageId.value !== null) {
    message.warning('当前有镜像正在导出打包中，请稍候...')
    return
  }

  exportingImageId.value = image.id
  const msgReactive = message.loading(`正在实时打包导出镜像 ${image.fullImageName}，请稍候...`, {
    duration: 0
  })

  try {
    const blob = await exportAdminImageApi(image.id)
    const safeRepo = image.repository.replace(/\//g, '_')
    const filename = `${safeRepo}_${image.tag}.tar`

    const downloadUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(downloadUrl)

    message.success(t('admin.images.backupModal.exportSuccess'))
  } catch (err) {
    console.error('Failed to export image:', err)
  } finally {
    msgReactive.destroy()
    exportingImageId.value = null
  }
}

async function handleSubmitBackup() {
  if (!backupFormRef.value || !selectedImage.value) return
  await backupFormRef.value.validate()

  if (backupForm.backupType === 'ARCHIVE') {
    const targetImage = selectedImage.value
    showBackupModal.value = false
    await handleExportImage(targetImage)
    return
  }

  submittingBackup.value = true
  try {
    await backupAdminImageApi({
      imageId: selectedImage.value.id,
      backupType: 'NODE',
      targetNodeIp: backupForm.targetNodeIp
    })
    message.info('镜像副本备份任务已提交后台异步执行，可前往【任务管理】实时追踪进度')
    showBackupModal.value = false
    await fetchImages()
    await fetchStatistics()
    await fetchClusterNodes()
  } catch {
    // handled by axios interceptor
  } finally {
    submittingBackup.value = false
  }
}

async function openDeleteModal(image: AdminClusterImageVO, defaultNodeIp?: string) {
  selectedDeleteImage.value = image
  await fetchClusterNodes()
  deleteScope.value = 'SPECIFIC'
  if (defaultNodeIp && image.nodeIps?.includes(defaultNodeIp)) {
    selectedDeleteNodes.value = [defaultNodeIp]
  } else if (image.nodeIps && image.nodeIps.length > 0) {
    selectedDeleteNodes.value = [image.nodeIps[0]]
  } else {
    selectedDeleteNodes.value = []
  }
  showDeleteModal.value = true
}

function toggleDeleteNode(ip: string) {
  const index = selectedDeleteNodes.value.indexOf(ip)
  if (index > -1) {
    selectedDeleteNodes.value.splice(index, 1)
  } else {
    selectedDeleteNodes.value.push(ip)
  }
}

function handleSelectAllDeleteNodes() {
  if (!selectedDeleteImage.value?.nodeIps) return
  selectedDeleteNodes.value = [...selectedDeleteImage.value.nodeIps]
}

function handleClearDeleteNodes() {
  selectedDeleteNodes.value = []
}

async function handleSubmitDelete() {
  if (!selectedDeleteImage.value) return

  if (deleteScope.value === 'SPECIFIC' && selectedDeleteNodes.value.length === 0) {
    message.warning(t('admin.images.deleteModal.selectNodesRequired'))
    return
  }

  submittingDelete.value = true
  try {
    const targetNodeIp =
      deleteScope.value === 'SPECIFIC'
        ? selectedDeleteNodes.value.join(',')
        : undefined

    await deleteAdminImageApi(selectedDeleteImage.value.id, targetNodeIp)

    if (deleteScope.value === 'SPECIFIC') {
      message.success(t('admin.images.deleteModal.successSpecific'))
    } else {
      message.success(t('admin.images.deleteModal.successAll'))
    }

    showDeleteModal.value = false
    await fetchImages()
    await fetchStatistics()
    await fetchClusterNodes()
  } catch {
    // handled by axios interceptor
  } finally {
    submittingDelete.value = false
  }
}

async function handleDeleteImage(image: AdminClusterImageVO) {
  try {
    const targetNodeIp = image.nodeIps?.[0]
    await deleteAdminImageApi(image.id, targetNodeIp)
    message.success(t('admin.images.deleteModal.successAll'))
    await fetchImages()
    await fetchStatistics()
    await fetchClusterNodes()
  } catch {
    // handled by axios interceptor
  }
}

async function handleSyncAllImages() {
  syncingAll.value = true
  try {
    await syncAllImagesToResourceNodesApi()
    message.success('已向所有资源节点广播全量题目镜像同步任务！')
    await fetchImages()
    await fetchStatistics()
    await fetchClusterNodes()
  } catch {
    message.error('同步题目镜像失败')
  } finally {
    syncingAll.value = false
  }
}

async function copyToClipboard(text: string) {
  try {
    await navigator.clipboard.writeText(text)
    message.success(t('admin.images.table.copied'))
  } catch {
    message.warning(text)
  }
}

async function handleClusterNodesUpdated() {
  await fetchClusterNodes()
  await fetchImages()
  await fetchStatistics()
}

function handleAsyncTaskCompleted(e: Event) {
  const customEvent = e as CustomEvent
  const task = customEvent.detail
  if (
    task &&
    (task.taskType === 'IMAGE_TRANSFER' ||
      task.taskType === 'IMAGE_PULL' ||
      task.taskType === 'IMAGE_SYNC' ||
      task.taskType === 'IMAGE_BACKUP')
  ) {
    fetchStatistics()
    fetchClusterNodes()
    fetchImages()
  }
}

onMounted(() => {
  fetchStatistics()
  fetchClusterNodes()
  fetchImages()
  window.addEventListener('volur:cluster-nodes-updated', handleClusterNodesUpdated)
  window.addEventListener('volur:async-task-completed', handleAsyncTaskCompleted)
})

onUnmounted(() => {
  window.removeEventListener('volur:cluster-nodes-updated', handleClusterNodesUpdated)
  window.removeEventListener('volur:async-task-completed', handleAsyncTaskCompleted)
})
</script>

<style scoped lang="scss">
.admin-image-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header {
  display: flex;
  flex-direction: column;
  gap: 4px;

  .page-title {
    font-size: 22px;
    font-weight: 700;
    margin: 0;
    color: var(--v-text-primary, #111111);
    letter-spacing: -0.3px;
  }

  .page-desc {
    font-size: 13px;
    color: #444444;
    margin: 0;
  }
}

/* Statistics Ribbon */
.stat-ribbon {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;

  .stat-mini-card {
    background-color: var(--n-color, #ffffff);
    border-radius: 10px;
    padding: 16px 20px;
    display: flex;
    align-items: center;
    gap: 14px;
    box-shadow: var(--v-shadow-sm, 0 1px 3px rgba(0, 0, 0, 0.04));
    transition: transform 0.2s ease;

    &:hover {
      transform: translateY(-2px);
    }

    .stat-icon-wrapper {
      width: 42px;
      height: 42px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;

      &.primary {
        background-color: rgba(37, 99, 235, 0.1);
        color: #2563eb;
      }

      &.warning {
        background-color: rgba(245, 158, 11, 0.1);
        color: #f59e0b;
      }

      &.info {
        background-color: rgba(99, 102, 241, 0.1);
        color: #6366f1;
      }

      &.success {
        background-color: rgba(16, 185, 129, 0.1);
        color: #10b981;
      }

      &.secondary {
        background-color: rgba(107, 114, 128, 0.1);
        color: #6b7280;
      }
    }

    .stat-info {
      display: flex;
      flex-direction: column;
      gap: 2px;

      .stat-value {
        font-size: 20px;
        font-weight: 700;
        color: var(--v-text-primary, #111111);
        line-height: 1.2;
      }

      .stat-label {
        font-size: 12px;
        color: #444444;
      }
    }
  }
}

.table-card {
  border-radius: 10px;
  background-color: var(--n-color, #ffffff);

  .filter-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 12px;
    margin-bottom: 16px;

    .filter-left {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 12px;
    }

    .filter-right {
      display: flex;
      align-items: center;
      gap: 12px;
    }
  }
}

.image-id-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  max-width: 270px;
}

.category-cell {
  display: flex;
  align-items: center;
  gap: 4px;
}

.mono-text {
  font-family: 'Fira Code', monospace;
  font-size: 12.5px;
  color: #333333;
}

.font-bold {
  font-weight: 600;
}

.truncate {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.time-text {
  font-size: 12px;
  color: #444444;
}

.node-tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.node-tooltip-content {
  font-size: 12px;
  line-height: 1.6;
}

.text-muted {
  color: #999999;
}

.table-action-btns {
  display: flex;
  align-items: center;
  gap: 4px;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.theme-primary-btn {
  background-color: #000000 !important;
  color: #ffffff !important;
  font-weight: 500;
  border-radius: 6px;
  border: none !important;

  &:hover {
    background-color: #222222 !important;
  }
}

.modal-subtext {
  font-size: 13px;
  color: #444444;
  margin: 0;
}

.modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.delete-scope-hint {
  display: block;
  font-size: 12px;
  color: #64748b;
  margin-top: 2px;
}

.delete-node-box {
  width: 100%;
}

.delete-node-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  margin-bottom: 8px;

  .node-select-hint {
    font-size: 12px;
    color: #64748b;
  }

  .action-btns {
    display: flex;
    align-items: center;
    gap: 4px;
  }
}

.delete-node-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.delete-node-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background-color: #fafbfc;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: #cbd5e1;
    background-color: #f1f5f9;
  }

  &.selected {
    border-color: #ef4444;
    background-color: #fef2f2;
  }

  .delete-node-info {
    display: flex;
    flex-direction: column;
    gap: 2px;
    flex: 1;
  }

  .delete-node-name {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    font-weight: 500;
    color: #1e293b;
  }

  .delete-node-ip {
    font-family: 'Fira Code', monospace;
    font-size: 12px;
    color: #64748b;
  }
}

/* Detail Drawer Styles */
.detail-loading-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  gap: 16px;

  .loading-subtext {
    font-size: 13px;
    color: #666666;
  }
}

.image-detail-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-header-card {
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;

  .header-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;

    .image-full-title {
      font-size: 16px;
      font-weight: 700;
      color: #0f172a;
      font-family: 'Fira Code', monospace;
    }
  }

  .header-meta-row {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 18px;
    font-size: 12.5px;

    .meta-item {
      display: flex;
      align-items: center;
      gap: 6px;

      .meta-label {
        color: #64748b;
      }
    }
  }
}

.layers-container {
  display: flex;
  flex-direction: column;
  gap: 12px;

  .layers-summary-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 13px;
    font-weight: 600;
    color: #475569;
    padding-bottom: 4px;

    .total-size-badge {
      font-family: 'Fira Code', monospace;
      color: #0f172a;
    }
  }
}

.layers-timeline {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.layer-item-card {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  transition: all 0.2s ease;

  &:hover {
    border-color: #cbd5e1;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  }

  .layer-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 12px;

    .layer-left {
      display: flex;
      align-items: center;
      gap: 8px;

      .layer-badge {
        font-family: 'Fira Code', monospace;
        font-weight: 700;
        color: #64748b;
        background-color: #f1f5f9;
        padding: 2px 6px;
        border-radius: 4px;
      }

      .instruction-tag {
        font-weight: 700;
        font-family: 'Fira Code', monospace;
      }

      .layer-id-text {
        font-family: 'Fira Code', monospace;
        color: #94a3b8;
        max-width: 140px;
      }
    }

    .layer-right {
      display: flex;
      align-items: center;
      gap: 12px;

      .layer-size {
        font-size: 12px;
        color: #0f172a;
        font-weight: 600;
      }

      .layer-time {
        color: #94a3b8;
        font-size: 11.5px;
      }
    }
  }

  .layer-command-box {
    background-color: #f8fafc;
    border-radius: 6px;
    padding: 8px 10px;
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 8px;
    position: relative;

    .command-text {
      font-family: 'Fira Code', monospace;
      font-size: 12px;
      line-height: 1.5;
      color: #1e293b;
      word-break: break-all;
      white-space: pre-wrap;
    }

    .copy-btn {
      opacity: 0.6;
      flex-shrink: 0;

      &:hover {
        opacity: 1;
      }
    }
  }
}

.inspect-container {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .inspect-section {
    display: flex;
    flex-direction: column;
    gap: 6px;

    .section-title {
      font-size: 13px;
      font-weight: 600;
      color: #334155;
    }

    .code-box {
      background-color: #f8fafc;
      border: 1px solid #e2e8f0;
      border-radius: 6px;
      padding: 10px 12px;
      font-family: 'Fira Code', monospace;
      font-size: 12px;
      color: #1e293b;
      word-break: break-all;
    }

    .tag-row {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
    }

    .env-list, .labels-list {
      display: flex;
      flex-direction: column;
      gap: 4px;
      background-color: #f8fafc;
      border: 1px solid #e2e8f0;
      border-radius: 6px;
      padding: 8px 12px;

      .env-row, .label-row {
        font-family: 'Fira Code', monospace;
        font-size: 12px;
        line-height: 1.6;
        word-break: break-all;

        .label-key {
          font-weight: 600;
          color: #2563eb;
          margin-right: 6px;
        }

        .label-val {
          color: #334155;
        }
      }
    }
  }
}

.basic-meta-container {
  .meta-table {
    display: flex;
    flex-direction: column;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    overflow: hidden;

    .meta-row {
      display: flex;
      align-items: center;
      padding: 12px 16px;
      border-bottom: 1px solid #f1f5f9;
      font-size: 13px;

      &:last-child {
        border-bottom: none;
      }

      .row-label {
        width: 190px;
        color: #64748b;
        flex-shrink: 0;
      }

      .row-value {
        color: #0f172a;
        word-break: break-all;
      }
    }
  }
}
</style>
