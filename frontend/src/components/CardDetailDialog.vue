<template>
  <q-dialog
    v-model="show"
    persistent
    maximized
    transition-show="slide-up"
    transition-hide="slide-down"
  >
    <q-card dark style="background: #111" class="column">
      <!-- Header -->
      <q-card-section
        class="row items-center q-py-sm"
        style="
          border-bottom: 1px solid rgba(255, 255, 255, 0.08);
          flex-shrink: 0;
        "
      >
        <q-icon name="task_alt" color="teal-5" class="q-mr-sm" />
        <div class="text-white text-weight-medium">Card Detail</div>
        <q-chip
          dense
          color="grey-8"
          text-color="grey-4"
          size="sm"
          class="q-ml-sm"
          >{{ detail?.stage }}</q-chip
        >
        <q-space />
        <q-btn flat round dense icon="delete" color="red-4" @click="confirmDeleteCard">
          <q-tooltip>Delete card</q-tooltip>
        </q-btn>
        <q-btn flat round dense icon="close" color="grey-5" v-close-popup />
      </q-card-section>

      <!-- Conflict banner: another user updated this card while it's open -->
      <div
        v-if="externalUpdate"
        class="conflict-banner row items-center q-px-lg q-py-xs"
      >
        <q-icon
          name="sync_problem"
          color="orange-4"
          size="18px"
          class="q-mr-sm"
        />
        <span style="font-size: 0.82rem">
          <b class="text-orange-3">{{ externalUpdate.actorName }}</b>
          <span class="text-grey-4">
            updated this card. Your edits may be outdated.</span
          >
        </span>
        <q-space />
        <q-btn
          flat
          dense
          label="Refresh"
          color="teal-4"
          size="sm"
          @click="refreshDetail"
        />
        <q-btn
          flat
          round
          dense
          icon="close"
          color="grey-6"
          size="xs"
          @click="$emit('dismiss-update')"
        />
      </div>

      <div class="col column" style="min-height: 0" v-if="detail">
        <!-- Cover image (full width, top) -->
        <div class="cover-image-wrap" style="flex-shrink: 0">
          <template v-if="uploadingCover">
            <div class="upload-indicator q-pa-md">
              <q-spinner-dots color="teal-5" size="20px" />
              <span class="text-grey-4" style="font-size:0.82rem">Uploading cover image...</span>
              <q-linear-progress indeterminate color="teal-5" size="2px" class="q-mt-xs" />
            </div>
          </template>
          <template v-else-if="detail.mainImageUrl">
            <img
              :src="detail.mainImageUrl"
              class="cover-image"
              style="cursor: pointer"
              @click="openSlideshow({ images: [detail.mainImageUrl], index: 0 })"
            />
            <div class="cover-image-actions">
              <q-btn
                flat dense icon="image" color="white" size="sm" label="Replace"
                style="background: rgba(0,0,0,0.55); border-radius: 6px"
                @click="showCoverPicker = true"
              />
              <q-btn
                flat dense icon="delete" color="red-3" size="sm" label="Remove"
                style="background: rgba(0,0,0,0.55); border-radius: 6px"
                @click="removeCoverImage"
              />
            </div>
          </template>
          <div v-else class="cover-image-empty">
            <q-btn
              flat dense icon="add_photo_alternate" color="grey-5"
              label="Add cover image" size="sm"
              @click="showCoverPicker = true"
            />
          </div>
          <input
            ref="coverImageInput"
            type="file"
            accept="image/*"
            style="display: none"
            @change="onCoverImageSelected"
          />
        </div>

        <!-- Cover image picker dialog -->
        <q-dialog v-model="showCoverPicker" position="top">
          <q-card dark style="background: #1e1e1e; width: 360px; border-radius: 10px; margin-top: 80px">
            <q-card-section class="q-pb-none q-pt-sm q-px-sm">
              <div class="row items-center">
                <span class="text-grey-3" style="font-size: 0.82rem; font-weight: 600">Cover image</span>
                <q-space />
                <q-btn flat round dense icon="close" color="grey-5" size="xs" v-close-popup />
              </div>
            </q-card-section>
            <q-card-section class="q-pt-sm q-px-sm q-pb-sm">
              <div v-if="coverPickerImages.length" class="cover-pick-grid q-mb-sm">
                <div
                  v-for="img in coverPickerImages"
                  :key="img.id"
                  class="cover-pick-item"
                  @click="setCoverFromExisting(img); showCoverPicker = false"
                >
                  <img :src="img.url" />
                </div>
              </div>
              <div v-else class="text-grey-6 text-center q-py-md" style="font-size: 0.78rem">
                No other images on this card.
              </div>
              <q-btn
                flat dense icon="upload" color="teal-4"
                label="Upload new" size="sm" class="full-width"
                @click="$refs.coverImageInput.click(); showCoverPicker = false"
              />
            </q-card-section>
          </q-card>
        </q-dialog>

        <div class="col row no-wrap justify-center" style="min-height: 0">
        <!-- LEFT: main content -->
        <div class="q-pa-lg" style="width: 680px; max-width: 680px; overflow-y: auto; min-height: 0">
          <!-- Card name -->
          <q-input
            v-model="detail.name"
            outlined
            dark
            color="teal-5"
            dense
            class="q-mb-md"
            label="Card name"
            @blur="saveField('name', detail.name)"
          />

          <!-- Metadata row: Status, Stage, Labels, Types, Members, Cover -->
          <div class="metadata-section q-mb-lg">
            <!-- Status + Stage row -->
            <div class="row q-gutter-md q-mb-sm items-end">
              <div style="min-width: 160px">
                <div class="sidebar-label">
                  <q-icon name="flag" size="xs" /> Status
                </div>
                <q-select
                  v-model="detail.status"
                  :options="statusOptions"
                  outlined
                  dark
                  dense
                  color="teal-5"
                />
              </div>
              <div style="min-width: 160px">
                <div class="sidebar-label">
                  <q-icon name="view_column" size="xs" /> Stage
                </div>
                <q-select
                  v-model="detail.columnId"
                  :options="stageOptions"
                  outlined
                  dark
                  dense
                  color="teal-5"
                  emit-value
                  map-options
                />
              </div>
              <q-btn
                v-if="hasStatusStageChanges"
                label="Save"
                color="teal-6"
                unelevated
                dense
                size="sm"
                :loading="savingStatusStage"
                style="margin-bottom: 1px"
                @click="saveStatusStage"
              />
            </div>

            <!-- Labels -->
            <div class="q-mb-sm">
              <q-btn flat dense color="grey-5" icon="label" label="Labels" size="sm">
                <q-menu
                  persistent
                  style="
                    min-width: 270px;
                    background: #1e1e1e;
                    border: 1px solid rgba(255, 255, 255, 0.1);
                  "
                >
                  <div class="q-pa-sm">
                    <!-- Header row -->
                    <div class="row items-center q-mb-sm">
                      <span
                        class="text-grey-4"
                        style="
                          font-size: 0.72rem;
                          font-weight: 600;
                          letter-spacing: 0.05em;
                        "
                        >BOARD LABELS</span
                      >
                      <q-space />
                      <q-btn
                        flat
                        round
                        dense
                        icon="close"
                        color="grey-5"
                        size="xs"
                        v-close-popup
                      />
                    </div>

                    <!-- Board labels: click to toggle attach/detach -->
                    <div
                      v-if="!allBoardLabels.length"
                      class="text-grey-6 text-caption q-mb-sm"
                    >
                      No labels yet — create one below.
                    </div>
                    <div
                      v-for="l in allBoardLabels"
                      :key="l.id"
                      class="row items-center q-mb-xs label-row"
                      style="
                        cursor: pointer;
                        border-radius: 6px;
                        padding: 2px 4px;
                      "
                      @click="toggleLabel(l)"
                    >
                      <q-chip
                        dense
                        :label="l.name"
                        :style="{
                          background: l.color,
                          color: l.textColor || '#fff',
                          minWidth: '110px',
                          fontSize: '0.78rem',
                        }"
                      />
                      <q-icon
                        v-if="isAttached(l)"
                        name="check"
                        color="teal-4"
                        size="16px"
                        class="q-ml-xs"
                      />
                      <q-space />
                      <q-btn
                        flat
                        round
                        dense
                        icon="delete"
                        color="grey-7"
                        size="xs"
                        @click.stop="deleteBoardLabel(l)"
                      />
                    </div>

                    <q-separator dark class="q-my-sm" />

                    <!-- Create new label form -->
                    <div
                      class="text-grey-5"
                      style="
                        font-size: 0.72rem;
                        font-weight: 600;
                        letter-spacing: 0.05em;
                        margin-bottom: 6px;
                      "
                    >
                      CREATE LABEL
                    </div>
                    <q-input
                      v-model="newLabel.name"
                      outlined
                      dark
                      color="teal-5"
                      dense
                      placeholder="Label name"
                      class="q-mb-sm"
                    />

                    <!-- 20 color preset swatches -->
                    <div
                      class="text-grey-6"
                      style="font-size: 0.7rem; margin-bottom: 4px"
                    >
                      Color presets
                    </div>
                    <div class="label-preset-grid q-mb-xs">
                      <div
                        v-for="(p, i) in allPresets"
                        :key="i"
                        class="label-preset-swatch"
                        :style="{ background: p.bg }"
                        :class="{
                          'preset-selected':
                            newLabel.bg === p.bg && newLabel.text === p.text,
                        }"
                        @click="
                          newLabel.bg = p.bg;
                          newLabel.text = p.text;
                        "
                        :title="`BG: ${p.bg}  Text: ${p.text}`"
                      />
                    </div>

                    <!-- Add custom color set -->
                    <div v-if="!showCustomPicker" class="q-mb-sm">
                      <q-btn
                        flat
                        dense
                        icon="add"
                        label="Add custom preset"
                        color="grey-5"
                        size="xs"
                        @click="showCustomPicker = true"
                      />
                    </div>
                    <div v-else class="q-mb-sm">
                      <div class="row items-center q-gutter-sm">
                        <div>
                          <div
                            class="text-grey-6"
                            style="font-size: 0.68rem; margin-bottom: 2px"
                          >
                            Background
                          </div>
                          <input
                            type="color"
                            v-model="customPreset.bg"
                            style="
                              width: 38px;
                              height: 26px;
                              border: 1px solid rgba(255, 255, 255, 0.2);
                              border-radius: 4px;
                              cursor: pointer;
                              padding: 2px;
                              background: #2a2a2a;
                            "
                          />
                        </div>
                        <div>
                          <div
                            class="text-grey-6"
                            style="font-size: 0.68rem; margin-bottom: 2px"
                          >
                            Text
                          </div>
                          <input
                            type="color"
                            v-model="customPreset.text"
                            style="
                              width: 38px;
                              height: 26px;
                              border: 1px solid rgba(255, 255, 255, 0.2);
                              border-radius: 4px;
                              cursor: pointer;
                              padding: 2px;
                              background: #2a2a2a;
                            "
                          />
                        </div>
                        <q-btn
                          flat
                          dense
                          icon="check"
                          color="teal-4"
                          size="sm"
                          style="margin-top: 14px"
                          title="Save to presets"
                          @click="saveCustomPreset"
                        />
                        <q-btn
                          flat
                          dense
                          icon="close"
                          color="grey-5"
                          size="sm"
                          style="margin-top: 14px"
                          @click="showCustomPicker = false"
                        />
                      </div>
                    </div>

                    <!-- Preview + create -->
                    <div class="row items-center q-gutter-xs q-mb-sm">
                      <span class="text-grey-6" style="font-size: 0.7rem"
                        >Preview:</span
                      >
                      <q-chip
                        dense
                        :label="newLabel.name || 'Label preview'"
                        :style="{
                          background: newLabel.bg,
                          color: newLabel.text,
                          fontSize: '0.78rem',
                        }"
                      />
                    </div>
                    <q-btn
                      label="Create label"
                      color="teal-6"
                      unelevated
                      dense
                      size="sm"
                      class="full-width"
                      :disable="!newLabel.name.trim()"
                      :loading="creatingLabel"
                      @click="createLabel"
                    />
                  </div>
                </q-menu>
              </q-btn>
              <div class="row q-gutter-xs q-mt-xs" style="flex-wrap: wrap">
                <q-chip
                  v-for="l in detail.labels"
                  :key="l.id"
                  dense
                  removable
                  :style="{
                    background: l.color,
                    color: l.textColor || '#fff',
                    fontSize: '0.72rem',
                  }"
                  @remove="removeLabel(l)"
                >
                  {{ l.name }}
                </q-chip>
              </div>
            </div>

            <!-- Types -->
            <div class="q-mb-sm">
              <q-btn flat dense color="grey-5" icon="category" label="Types" size="sm">
                <q-menu
                  persistent
                  style="
                    min-width: 270px;
                    background: #1e1e1e;
                    border: 1px solid rgba(255, 255, 255, 0.1);
                  "
                >
                  <div class="q-pa-sm">
                    <!-- Header row -->
                    <div class="row items-center q-mb-sm">
                      <span
                        class="text-grey-4"
                        style="
                          font-size: 0.72rem;
                          font-weight: 600;
                          letter-spacing: 0.05em;
                        "
                        >BOARD TYPES</span
                      >
                      <q-space />
                      <q-btn
                        flat
                        round
                        dense
                        icon="close"
                        color="grey-5"
                        size="xs"
                        v-close-popup
                      />
                    </div>

                    <!-- Board types: click to toggle attach/detach -->
                    <div
                      v-if="!allBoardTypes.length"
                      class="text-grey-6 text-caption q-mb-sm"
                    >
                      No types yet — create one below.
                    </div>
                    <div
                      v-for="t in allBoardTypes"
                      :key="t.id"
                      class="row items-center q-mb-xs label-row"
                      style="
                        cursor: pointer;
                        border-radius: 6px;
                        padding: 2px 4px;
                      "
                      @click="toggleType(t)"
                    >
                      <q-chip
                        dense
                        :label="t.name"
                        :style="{
                          background: t.color,
                          color: t.textColor || '#fff',
                          minWidth: '110px',
                          fontSize: '0.78rem',
                        }"
                      />
                      <q-icon
                        v-if="isTypeAttached(t)"
                        name="check"
                        color="teal-4"
                        size="16px"
                        class="q-ml-xs"
                      />
                      <q-space />
                      <q-btn
                        flat
                        round
                        dense
                        icon="delete"
                        color="grey-7"
                        size="xs"
                        @click.stop="deleteBoardType(t)"
                      />
                    </div>

                    <q-separator dark class="q-my-sm" />

                    <!-- Create new type form -->
                    <div
                      class="text-grey-5"
                      style="
                        font-size: 0.72rem;
                        font-weight: 600;
                        letter-spacing: 0.05em;
                        margin-bottom: 6px;
                      "
                    >
                      CREATE TYPE
                    </div>
                    <q-input
                      v-model="newType.name"
                      outlined
                      dark
                      color="teal-5"
                      dense
                      placeholder="Type name"
                      class="q-mb-sm"
                    />

                    <!-- 20 color preset swatches -->
                    <div
                      class="text-grey-6"
                      style="font-size: 0.7rem; margin-bottom: 4px"
                    >
                      Color presets
                    </div>
                    <div class="label-preset-grid q-mb-xs">
                      <div
                        v-for="(p, i) in allPresets"
                        :key="'type-' + i"
                        class="label-preset-swatch"
                        :style="{ background: p.bg }"
                        :class="{
                          'preset-selected':
                            newType.bg === p.bg && newType.text === p.text,
                        }"
                        @click="
                          newType.bg = p.bg;
                          newType.text = p.text;
                        "
                        :title="`BG: ${p.bg}  Text: ${p.text}`"
                      />
                    </div>

                    <!-- Preview + create -->
                    <div class="row items-center q-gutter-xs q-mb-sm">
                      <span class="text-grey-6" style="font-size: 0.7rem"
                        >Preview:</span
                      >
                      <q-chip
                        dense
                        :label="newType.name || 'Type preview'"
                        :style="{
                          background: newType.bg,
                          color: newType.text,
                          fontSize: '0.78rem',
                        }"
                      />
                    </div>
                    <q-btn
                      label="Create type"
                      color="teal-6"
                      unelevated
                      dense
                      size="sm"
                      class="full-width"
                      :disable="!newType.name.trim()"
                      :loading="creatingType"
                      @click="createType"
                    />
                  </div>
                </q-menu>
              </q-btn>
              <div class="row q-gutter-xs q-mt-xs" style="flex-wrap: wrap">
                <q-chip
                  v-for="t in detail.types"
                  :key="t.id"
                  dense
                  removable
                  :style="{
                    background: t.color,
                    color: t.textColor || '#fff',
                    fontSize: '0.72rem',
                  }"
                  @remove="removeType(t)"
                >
                  {{ t.name }}
                </q-chip>
              </div>
            </div>

            <!-- Members -->
            <div class="q-mb-sm">
              <q-btn flat dense color="grey-5" icon="group" label="Members" size="sm">
                <q-menu
                  persistent
                  style="
                    min-width: 270px;
                    background: #1e1e1e;
                    border: 1px solid rgba(255, 255, 255, 0.1);
                  "
                >
                  <div class="q-pa-sm">
                    <div class="row items-center q-mb-sm">
                      <span
                        class="text-grey-4"
                        style="
                          font-size: 0.72rem;
                          font-weight: 600;
                          letter-spacing: 0.05em;
                        "
                        >BOARD MEMBERS</span
                      >
                      <q-space />
                      <q-btn
                        flat round dense icon="close" color="grey-5" size="xs"
                        v-close-popup
                      />
                    </div>
                    <div
                      v-if="!allBoardMemberUsers.length"
                      class="text-grey-6 text-caption q-mb-sm"
                    >
                      No members on this board.
                    </div>
                    <div
                      v-for="u in allBoardMemberUsers"
                      :key="u.id"
                      class="row items-center q-mb-xs label-row"
                      style="
                        cursor: pointer;
                        border-radius: 6px;
                        padding: 4px 6px;
                      "
                      @click="toggleMember(u)"
                    >
                      <UserAvatar :user="u" size="24px" />
                      <span class="text-grey-3 q-ml-sm" style="font-size: 0.82rem">
                        {{ u.firstName }} {{ u.lastName }}
                      </span>
                      <q-icon
                        v-if="isMemberAttached(u)"
                        name="check"
                        color="teal-4"
                        size="16px"
                        class="q-ml-xs"
                      />
                    </div>
                  </div>
                </q-menu>
              </q-btn>
              <div class="row q-gutter-xs q-mt-xs items-center">
                <div v-for="m in detail.members" :key="m.id" :title="m.userName">
                  <UserAvatar :user="m" size="28px" />
                </div>
              </div>
            </div>

          </div>

          <!-- Design Detail Section (POD_DESIGN boards only) -->
          <DesignDetailSection
            v-if="props.boardType === 'POD_DESIGN' && detail?.id"
            :card-id="detail.id"
            :board-members="props.boardMembers"
            @updated="emit('updated')"
            @view-images="openSlideshow($event)"
          />

          <!-- Description -->
          <div class="text-caption text-grey-5 q-mb-xs row items-center gap-2">
            <q-icon name="subject" /> Description
          </div>
          <div
            class="desc-editor-wrap"
            :class="{ 'desc-collapsed': !descExpanded }"
            @mousemove="onDescMouseMove"
            @mouseleave="hideDescImgOverlay"
            @click="expandDesc"
          >
            <q-editor
              ref="descEditor"
              v-model="detail.description"
              dark
              min-height="120px"
              class="desc-editor"
              :class="{ 'q-mb-lg': descExpanded }"
              style="
                background: #1a1a1a;
                border: 1px solid rgba(255, 255, 255, 0.1);
                border-radius: 6px;
              "
              :toolbar="[
                ['bold', 'italic', 'underline', 'strike'],
                ['unordered', 'ordered'],
                ['link', 'fullscreen'],
                ['insert_image'],
              ]"
              :definitions="{ insert_image: { icon: 'image', tip: 'Insert image', handler: triggerDescImageUpload } }"
              @blur="onDescBlur"
            />
            <!-- Fade overlay + expand bar when collapsed -->
            <div v-if="!descExpanded && descOverflows" class="desc-fade-overlay" @click="expandDesc">
              <div class="desc-expand-bar">
                <q-icon name="unfold_more" size="18px" />
                <span>Show more</span>
                <q-icon name="unfold_more" size="18px" />
              </div>
            </div>
            <!-- Floating overlay for description images -->
            <div
              v-if="descImgOverlay.visible"
              class="desc-img-overlay"
              :style="descImgOverlay.style"
              @mouseenter="descImgOverlay.locked = true"
              @mouseleave="hideDescImgOverlay"
            >
              <q-btn
                flat round dense icon="zoom_in" color="white" size="sm"
                @click.stop="viewDescImage"
              >
                <q-tooltip>View</q-tooltip>
              </q-btn>
              <q-btn
                flat round dense icon="delete" color="red-4" size="sm"
                @click.stop="deleteDescImage"
              >
                <q-tooltip>Delete</q-tooltip>
              </q-btn>
            </div>
          </div>
          <!-- Collapse bar when expanded -->
          <div v-if="descExpanded && descOverflows" class="desc-collapse-bar q-mb-lg" @click="descExpanded = false">
            <q-icon name="unfold_less" size="18px" />
            <span>Show less</span>
            <q-icon name="unfold_less" size="18px" />
          </div>
          <div v-if="!descExpanded" class="q-mb-lg" />
          <input
            ref="descImageInput"
            type="file"
            accept="image/*"
            multiple
            style="display: none"
            @change="onDescImageSelected"
          />

          <!-- Links -->
          <div class="q-mb-md">
            <div class="text-caption text-grey-5 q-mb-xs row items-center gap-2">
              <q-icon name="link" /> Links
            </div>
            <q-list v-if="detail.links?.length" dense class="q-mb-sm">
              <q-item v-for="lnk in detail.links" :key="lnk.id" dense>
                <q-item-section avatar>
                  <q-icon name="open_in_new" color="teal-4" />
                </q-item-section>
                <q-item-section>
                  <a
                    :href="lnk.url"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="text-teal-4 link-truncate"
                    style="text-decoration: none"
                  >{{ lnk.title || lnk.url }}</a>
                  <q-item-label caption class="text-grey-6 link-truncate">{{ lnk.url }}</q-item-label>
                </q-item-section>
                <q-item-section side>
                  <q-btn
                    flat round dense icon="delete" color="red-4" size="xs"
                    @click="deleteLink(lnk)"
                  />
                </q-item-section>
              </q-item>
            </q-list>
            <div class="row q-gutter-sm items-end">
              <q-input
                v-model="newLink.url"
                outlined dark color="teal-5" dense
                placeholder="https://..."
                class="col"
                label="URL"
              />
              <q-input
                v-model="newLink.title"
                outlined dark color="teal-5" dense
                placeholder="Title (optional)"
                style="max-width: 180px"
              />
              <q-btn
                icon="add_link"
                color="teal-6"
                unelevated dense
                :loading="addingLink"
                :disable="!newLink.url.trim()"
                @click="addLink"
              />
            </div>
          </div>

          <!-- Images -->
          <div class="q-mb-md">
            <div class="text-caption text-grey-5 q-mb-xs row items-center gap-2">
              <q-icon name="image" /> Images
            </div>
            <div v-if="imageAttachments.length" class="image-grid q-mb-sm">
              <div
                v-for="img in imageAttachments"
                :key="img.id"
                class="image-thumb-wrap"
              >
                <img
                  :src="img.url"
                  class="image-thumb"
                  @click.stop="openSlideshow({ images: [img.url], index: 0 })"
                />
                <div class="image-thumb-actions">
                  <q-btn
                    flat round dense icon="download" color="white" size="xs"
                    @click.stop="downloadFile(img.url, img.name)"
                  />
                  <q-btn
                    flat round dense icon="close" color="red-4" size="xs"
                    @click.stop="deleteAttachment(img)"
                  />
                </div>
                <div class="image-thumb-name text-grey-5">{{ img.name }}</div>
              </div>
            </div>
            <div v-if="uploadingImages" class="upload-indicator">
              <q-spinner-dots color="teal-5" size="20px" />
              <span class="text-grey-4" style="font-size:0.82rem">Uploading images...</span>
              <q-linear-progress indeterminate color="teal-5" size="2px" class="q-mt-xs" />
            </div>
            <q-file
              v-else
              v-model="imageFile"
              outlined
              dark
              color="teal-5"
              dense
              multiple
              label="Upload images"
              accept="image/*"
              @update:model-value="uploadImages"
            >
              <template #prepend
                ><q-icon name="add_photo_alternate" color="grey-5"
              /></template>
            </q-file>
          </div>

          <!-- Files -->
          <div class="q-mb-lg">
            <div class="text-caption text-grey-5 q-mb-xs row items-center gap-2">
              <q-icon name="attach_file" /> Files
            </div>
            <q-list v-if="fileAttachments.length" dense class="q-mb-sm">
              <q-item v-for="att in fileAttachments" :key="att.id" dense>
                <q-item-section avatar>
                  <q-icon name="insert_drive_file" color="teal-4" />
                </q-item-section>
                <q-item-section>
                  <a
                    :href="att.url"
                    target="_blank"
                    class="text-teal-4"
                    style="text-decoration: none"
                    >{{ att.name }}</a
                  >
                  <q-item-label caption class="text-grey-6"
                    >{{ att.fileType }} · {{ att.createdBy }}</q-item-label
                  >
                </q-item-section>
                <q-item-section side>
                  <q-btn
                    flat
                    round
                    dense
                    icon="delete"
                    color="red-4"
                    size="xs"
                    @click="deleteAttachment(att)"
                  />
                </q-item-section>
              </q-item>
            </q-list>
            <div v-if="uploadingFile" class="upload-indicator">
              <q-spinner-dots color="teal-5" size="20px" />
              <span class="text-grey-4" style="font-size:0.82rem">Uploading file...</span>
              <q-linear-progress indeterminate color="teal-5" size="2px" class="q-mt-xs" />
            </div>
            <q-file
              v-else
              v-model="attachFile"
              outlined
              dark
              color="teal-5"
              dense
              label="Attach a file"
              @update:model-value="uploadAttachment"
            >
              <template #prepend
                ><q-icon name="upload_file" color="grey-5"
              /></template>
            </q-file>
          </div>

          <!-- Activity -->
          <div
            class="text-caption text-grey-5 q-mt-lg q-mb-sm row items-center gap-2"
          >
            <q-icon name="history" /> Activity
          </div>
          <div
            v-for="act in detail.activities"
            :key="act.id"
            class="row items-center q-gutter-xs q-mb-xs"
          >
            <q-icon name="circle" size="6px" color="teal-4" />
            <span class="text-grey-5" style="font-size: 0.78rem">{{
              act.action
            }}</span>
            <span class="text-grey-7" style="font-size: 0.72rem"
              >· {{ formatDate(act.createdAt) }}</span
            >
          </div>
        </div>

        <!-- RIGHT: Comments -->
        <div
          class="q-pa-lg"
          style="
            width: 420px;
            flex-shrink: 0;
            border-left: 1px solid rgba(255, 255, 255, 0.07);
            overflow-y: auto;
            min-height: 0;
          "
        >
          <div class="text-caption text-grey-5 q-mb-sm row items-center gap-2">
            <q-icon name="chat" /> Comments
          </div>
          <CommentItem
            v-for="c in detail.comments"
            :key="c.id"
            :comment="c"
            :current-user="authStore.currentUser"
            :is-admin="authStore.isAdmin"
            :members="allBoardMemberUsers"
            @delete="deleteComment($event)"
            @react="handleReaction($event)"
            @reply="replyTo = $event"
            @view-images="openSlideshow($event)"
          />
          <CommentInput
            :card-id="detail.id"
            :taggable-users="taggableUsers"
            :reply-to="replyTo"
            @commented="onCommented"
            @cancel-reply="replyTo = null"
          />
        </div>
        </div>
      </div>

      <div v-else class="col flex flex-center">
        <q-spinner color="teal-5" size="40px" />
      </div>
    </q-card>

    <!-- Image lightbox / slideshow -->
    <q-dialog v-model="showLightbox" maximized transition-show="fade" transition-hide="fade">
      <div class="lightbox-backdrop" @click="closeLightbox" @keydown="onLightboxKeydown" tabindex="0" ref="lightboxBackdrop">
        <img :src="lightboxUrl" class="lightbox-img" @click.stop />
        <!-- Left / right arrows (only when slideshow has multiple images) -->
        <template v-if="slideshowImages.length > 1">
          <q-btn
            flat round icon="chevron_left" color="white" size="lg"
            class="slideshow-arrow slideshow-arrow-left"
            @click.stop="slideshowPrev"
          />
          <q-btn
            flat round icon="chevron_right" color="white" size="lg"
            class="slideshow-arrow slideshow-arrow-right"
            @click.stop="slideshowNext"
          />
        </template>
        <!-- Thumbnail strip -->
        <div v-if="slideshowImages.length > 1" class="lightbox-thumbstrip" @click.stop>
          <div v-for="(url, i) in slideshowImages" :key="i"
            class="lightbox-thumb" :class="{ active: i === slideshowIndex }"
            @click="slideshowIndex = i">
            <img :src="url" />
          </div>
        </div>
        <div class="lightbox-toolbar" @click.stop>
          <q-btn
            flat round dense icon="download" color="white" size="md"
            @click="downloadFile(lightboxUrl, lightboxFilename)"
          >
            <q-tooltip>Download</q-tooltip>
          </q-btn>
          <q-btn
            flat round dense icon="close" color="white" size="md"
            @click="closeLightbox"
          >
            <q-tooltip>Close</q-tooltip>
          </q-btn>
        </div>
      </div>
    </q-dialog>
  </q-dialog>
</template>

<script setup>
import { ref, computed, watch, onBeforeUnmount, nextTick } from "vue";
import { useQuasar } from "quasar";
import { cardApi, boardApi } from "src/api/tasks";
import { useAuthStore } from "src/stores/authStore";
import UserAvatar from "src/components/UserAvatar.vue";
import CommentItem from "src/components/CommentItem.vue";
import CommentInput from "src/components/CommentInput.vue";
import DesignDetailSection from "src/components/DesignDetailSection.vue";

// ─── Label color presets ──────────────────────────────────────────────────────
const LABEL_PRESETS = [
  { bg: "#e53935", text: "#ffffff" },
  { bg: "#d81b60", text: "#ffffff" },
  { bg: "#8e24aa", text: "#ffffff" },
  { bg: "#5e35b1", text: "#ffffff" },
  { bg: "#3949ab", text: "#ffffff" },
  { bg: "#1e88e5", text: "#ffffff" },
  { bg: "#039be5", text: "#ffffff" },
  { bg: "#00acc1", text: "#ffffff" },
  { bg: "#00897b", text: "#ffffff" },
  { bg: "#43a047", text: "#ffffff" },
  { bg: "#7cb342", text: "#ffffff" },
  { bg: "#c0ca33", text: "#212121" },
  { bg: "#fdd835", text: "#212121" },
  { bg: "#ffb300", text: "#212121" },
  { bg: "#fb8c00", text: "#ffffff" },
  { bg: "#f4511e", text: "#ffffff" },
  { bg: "#6d4c41", text: "#ffffff" },
  { bg: "#546e7a", text: "#ffffff" },
  { bg: "#455a64", text: "#ffffff" },
  { bg: "#2e7d32", text: "#ffffff" },
];
const CUSTOM_PRESETS_KEY = "kanban_label_custom_presets";
const loadCustomPresets = () => {
  try {
    return JSON.parse(localStorage.getItem(CUSTOM_PRESETS_KEY) || "[]");
  } catch {
    return [];
  }
};

const props = defineProps({
  modelValue: Boolean,
  cardId: { type: Number, default: null },
  boardId: { type: Number, default: null },
  boardType: { type: String, default: 'GENERAL' },
  boardLabels: { type: Array, default: () => [] },
  boardTypes: { type: Array, default: () => [] },
  boardMembers: { type: Array, default: () => [] },
  boardColumns: { type: Array, default: () => [] },
  externalUpdate: { type: Object, default: null }, // { actorName, type }
});
const emit = defineEmits(["update:modelValue", "updated", "deleted", "dismiss-update"]);
const $q = useQuasar();
const authStore = useAuthStore();

const show = computed({
  get: () => props.modelValue,
  set: (v) => emit("update:modelValue", v),
});
const detail = ref(null);
const replyTo = ref(null);
const attachFile = ref(null);
const imageFile = ref(null);
const uploadingImages = ref(false);
const uploadingFile = ref(false);
const newLink = ref({ url: '', title: '' });
const showCoverPicker = ref(false);
const uploadingCover = ref(false);
const addingLink = ref(false);
const descEditor = ref(null);
const descImageInput = ref(null);
let savedSelection = null;

// ─── Description expand / collapse ───────────────────────────────────────────
const descExpanded = ref(false);
const descOverflows = ref(false);
let _descResizeObserver = null;

const checkDescOverflow = () => {
  const el = descEditor.value?.$el?.querySelector('.q-editor__content');
  if (el) descOverflows.value = el.scrollHeight > 500;
};

const scheduleOverflowCheck = () => {
  // q-editor needs time to render content into contenteditable
  nextTick(() => setTimeout(checkDescOverflow, 150));
};

const setupDescResizeObserver = () => {
  cleanupDescResizeObserver();
  const el = descEditor.value?.$el?.querySelector('.q-editor__content');
  if (!el) return;
  _descResizeObserver = new ResizeObserver(checkDescOverflow);
  _descResizeObserver.observe(el);
};

const cleanupDescResizeObserver = () => {
  if (_descResizeObserver) {
    _descResizeObserver.disconnect();
    _descResizeObserver = null;
  }
};

const expandDesc = () => {
  if (!descExpanded.value) descExpanded.value = true;
};

const onDescBlur = () => {
  saveField('description', detail.value.description);
  scheduleOverflowCheck();
};
// ─────────────────────────────────────────────────────────────────────────────

// ─── Description image hover overlay ─────────────────────────────────────────
const descImgOverlay = ref({ visible: false, locked: false, style: {}, targetImg: null });

const onDescMouseMove = (e) => {
  if (descImgOverlay.value.locked) return;
  const img = e.target.closest?.('.q-editor__content img') || (e.target.tagName === 'IMG' && e.target.closest('.q-editor__content') ? e.target : null);
  if (img) {
    const wrapEl = e.currentTarget;
    const wrapRect = wrapEl.getBoundingClientRect();
    const imgRect = img.getBoundingClientRect();
    descImgOverlay.value = {
      visible: true,
      locked: false,
      targetImg: img,
      style: {
        top: (imgRect.top - wrapRect.top) + 'px',
        left: (imgRect.left - wrapRect.left) + 'px',
        width: imgRect.width + 'px',
        height: imgRect.height + 'px',
      },
    };
  } else if (!descImgOverlay.value.locked) {
    descImgOverlay.value.visible = false;
  }
};

const hideDescImgOverlay = () => {
  descImgOverlay.value = { visible: false, locked: false, style: {}, targetImg: null };
};

const viewDescImage = () => {
  const img = descImgOverlay.value.targetImg;
  if (img) openSlideshow({ images: [img.src], index: 0 });
  hideDescImgOverlay();
};

const deleteDescImage = () => {
  const img = descImgOverlay.value.targetImg;
  if (img) {
    img.remove();
    const editorEl = descEditor.value?.$el?.querySelector('.q-editor__content');
    if (editorEl) {
      detail.value.description = editorEl.innerHTML;
      saveField('description', detail.value.description);
    }
  }
  hideDescImgOverlay();
};
// ─────────────────────────────────────────────────────────────────────────────

const saveCaretPosition = () => {
  const sel = window.getSelection();
  if (sel.rangeCount > 0) {
    savedSelection = sel.getRangeAt(0).cloneRange();
  }
};

const restoreCaretPosition = () => {
  if (savedSelection) {
    const sel = window.getSelection();
    sel.removeAllRanges();
    sel.addRange(savedSelection);
    savedSelection = null;
  }
};

const insertHtmlAtCaret = (html) => {
  restoreCaretPosition();
  const sel = window.getSelection();
  if (!sel.rangeCount) {
    // Fallback: append to end of editor content
    detail.value.description = (detail.value.description || "") + html;
    return;
  }
  const range = sel.getRangeAt(0);
  range.deleteContents();
  const temp = document.createElement("div");
  temp.innerHTML = html;
  const frag = document.createDocumentFragment();
  let lastNode;
  while (temp.firstChild) {
    lastNode = frag.appendChild(temp.firstChild);
  }
  range.insertNode(frag);
  if (lastNode) {
    const newRange = document.createRange();
    newRange.setStartAfter(lastNode);
    newRange.collapse(true);
    sel.removeAllRanges();
    sel.addRange(newRange);
  }
};

const uploadAndInsertImage = async (file) => {
  if (!file || !detail.value) return;
  // Show inline placeholder while uploading
  const placeholderId = "img-ph-" + Date.now();
  insertHtmlAtCaret(
    `<span id="${placeholderId}" style="display:inline-block;background:#2a2a2a;border-radius:6px;padding:8px 16px;margin:8px 0;color:#888;font-size:0.8rem">Uploading image...</span>`
  );
  // Sync model with contenteditable
  const editorEl = descEditor.value?.$el?.querySelector(".q-editor__content");
  if (editorEl) detail.value.description = editorEl.innerHTML;

  const fd = new FormData();
  fd.append("file", file);
  try {
    const res = await cardApi.uploadAttachment(detail.value.id, fd);
    const url = res.data.data.url;
    // Replace placeholder with actual image
    const imgTag = `<img src="${url}" style="display:block;max-width:66%;height:auto;border-radius:6px;margin:8px 0" />`;
    detail.value.description = detail.value.description.replace(
      new RegExp(`<span[^>]*id="${placeholderId}"[^>]*>.*?</span>`),
      imgTag
    );
    saveField("description", detail.value.description);
  } catch {
    // Remove placeholder on failure
    detail.value.description = detail.value.description.replace(
      new RegExp(`<span[^>]*id="${placeholderId}"[^>]*>.*?</span>`),
      ""
    );
    $q.notify({ type: "negative", message: "Image upload failed" });
  }
};

const triggerDescImageUpload = () => {
  saveCaretPosition();
  descImageInput.value?.click();
};

const onDescImageSelected = (e) => {
  const files = e.target.files;
  if (!files?.length) return;
  for (const file of files) {
    uploadAndInsertImage(file);
  }
  e.target.value = "";
};

const onDescPaste = (e) => {
  const items = e.clipboardData?.items;
  if (!items) return;
  for (const item of items) {
    if (item.type.startsWith("image/")) {
      e.preventDefault();
      e.stopImmediatePropagation();
      saveCaretPosition();
      uploadAndInsertImage(item.getAsFile());
      return;
    }
  }
};

const onDescDrop = (e) => {
  e.preventDefault();
  e.stopImmediatePropagation();
  const files = e.dataTransfer?.files;
  if (!files) return;
  for (const file of files) {
    if (file.type.startsWith("image/")) {
      uploadAndInsertImage(file);
      return;
    }
  }
};

// Attach paste/drop listeners directly on the contenteditable element
// so we intercept BEFORE q-editor's own handling
let _contentEl = null;
const attachEditorListeners = () => {
  const el = descEditor.value?.$el?.querySelector('.q-editor__content');
  if (el && el !== _contentEl) {
    if (_contentEl) {
      _contentEl.removeEventListener('paste', onDescPaste, true);
      _contentEl.removeEventListener('drop', onDescDrop, true);
      _contentEl.removeEventListener('dragover', preventDragover, true);
    }
    _contentEl = el;
    el.addEventListener('paste', onDescPaste, true);
    el.addEventListener('drop', onDescDrop, true);
    el.addEventListener('dragover', preventDragover, true);
  }
};
const preventDragover = (e) => e.preventDefault();

watch(() => [props.modelValue, detail.value], () => {
  if (props.modelValue && detail.value) {
    nextTick(attachEditorListeners);
  }
});

onBeforeUnmount(() => {
  if (_contentEl) {
    _contentEl.removeEventListener('paste', onDescPaste, true);
    _contentEl.removeEventListener('drop', onDescDrop, true);
    _contentEl.removeEventListener('dragover', preventDragover, true);
  }
  cleanupDescResizeObserver();
});

const statusOptions = ["OPEN", "IN_PROGRESS", "DONE", "BLOCKED", "CANCELLED"];

const stageOptions = computed(() =>
  props.boardColumns
    .slice()
    .sort((a, b) => a.position - b.position)
    .map((col) => ({ label: col.title, value: col.id }))
);

// ─── Status / Stage save with button ─────────────────────────────────────────
const originalStatus = ref(null);
const originalColumnId = ref(null);
const savingStatusStage = ref(false);

const hasStatusStageChanges = computed(() =>
  detail.value &&
  (detail.value.status !== originalStatus.value ||
   detail.value.columnId !== originalColumnId.value)
);

const saveStatusStage = async () => {
  if (!detail.value || !hasStatusStageChanges.value) return;
  savingStatusStage.value = true;
  try {
    // Save status if changed
    if (detail.value.status !== originalStatus.value) {
      await cardApi.update(detail.value.id, { status: detail.value.status });
      originalStatus.value = detail.value.status;
    }
    // Move card if stage changed
    if (detail.value.columnId !== originalColumnId.value) {
      await cardApi.move(detail.value.id, { targetColumnId: detail.value.columnId, position: 0 });
      const col = props.boardColumns.find((c) => c.id === detail.value.columnId);
      if (col) detail.value.stage = col.title;
      originalColumnId.value = detail.value.columnId;
    }
    emit("updated");
  } catch {
    $q.notify({ type: "negative", message: "Failed to save changes" });
  } finally {
    savingStatusStage.value = false;
  }
};
// ─────────────────────────────────────────────────────────────────────────────

const IMAGE_TYPES = ["image/png", "image/jpeg", "image/jpg", "image/gif", "image/webp", "image/svg+xml"];
const isImage = (att) => IMAGE_TYPES.includes(att.fileType?.toLowerCase());
const imageAttachments = computed(() => {
  const all = detail.value?.attachments?.filter(isImage) || [];
  const desc = detail.value?.description || '';
  return all.filter((img) => !desc.includes(img.url));
});
const allCardImages = computed(() => detail.value?.attachments?.filter(isImage) || []);
const coverPickerImages = computed(() =>
  allCardImages.value.filter((img) => img.url !== detail.value?.mainImageUrl)
);
const fileAttachments = computed(() => detail.value?.attachments?.filter((a) => !isImage(a)) || []);
// ─── Lightbox / slideshow state ──────────────────────────────────────────────
const slideshowImages = ref([]);
const slideshowIndex = ref(0);
const lightboxBackdrop = ref(null);
const lightboxUrl = computed(() =>
  slideshowImages.value.length ? slideshowImages.value[slideshowIndex.value] : null
);
const showLightbox = computed({
  get: () => slideshowImages.value.length > 0,
  set: (v) => { if (!v) slideshowImages.value = []; },
});
const lightboxFilename = computed(() => {
  if (!lightboxUrl.value) return "image";
  const img = imageAttachments.value.find((a) => a.url === lightboxUrl.value);
  return img?.name || lightboxUrl.value.split("/").pop();
});
const openSlideshow = ({ images, index }) => {
  slideshowImages.value = images;
  slideshowIndex.value = index || 0;
  nextTick(() => lightboxBackdrop.value?.focus());
};
const closeLightbox = () => {
  slideshowImages.value = [];
  slideshowIndex.value = 0;
};
const slideshowPrev = () => {
  if (slideshowImages.value.length <= 1) return;
  slideshowIndex.value = (slideshowIndex.value - 1 + slideshowImages.value.length) % slideshowImages.value.length;
};
const slideshowNext = () => {
  if (slideshowImages.value.length <= 1) return;
  slideshowIndex.value = (slideshowIndex.value + 1) % slideshowImages.value.length;
};
const onLightboxKeydown = (e) => {
  if (e.key === 'ArrowLeft') slideshowPrev();
  else if (e.key === 'ArrowRight') slideshowNext();
  else if (e.key === 'Escape') closeLightbox();
};

const downloadFile = async (url, filename) => {
  try {
    const res = await fetch(url);
    const blob = await res.blob();
    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = filename || url.split("/").pop();
    a.click();
    URL.revokeObjectURL(a.href);
  } catch {
    $q.notify({ type: "negative", message: "Download failed" });
  }
};

// ─── Label management state ───────────────────────────────────────────────────
// Local copy of board labels so we can push newly created ones immediately
const allBoardLabels = ref([]);
watch(
  () => props.boardLabels,
  (v) => {
    allBoardLabels.value = [...v];
  },
  { immediate: true },
);

const allPresets = ref([...LABEL_PRESETS, ...loadCustomPresets()]);
const newLabel = ref({
  name: "",
  bg: LABEL_PRESETS[0].bg,
  text: LABEL_PRESETS[0].text,
});
const creatingLabel = ref(false);
const showCustomPicker = ref(false);
const customPreset = ref({ bg: "#3949ab", text: "#ffffff" });

const isAttached = (label) =>
  detail.value?.labels?.some((l) => l.id === label.id);

const toggleLabel = async (label) => {
  if (isAttached(label)) await removeLabel(label);
  else await addLabel(label);
};

const createLabel = async () => {
  if (!newLabel.value.name.trim() || !props.boardId) return;
  creatingLabel.value = true;
  try {
    const res = await boardApi.createLabel(props.boardId, {
      name: newLabel.value.name.trim(),
      color: newLabel.value.bg,
      textColor: newLabel.value.text,
    });
    allBoardLabels.value.push(res.data.data);
    newLabel.value.name = "";
    emit("updated");
  } catch {
    $q.notify({ type: "negative", message: "Failed to create label" });
  } finally {
    creatingLabel.value = false;
  }
};

const deleteBoardLabel = async (label) => {
  try {
    await boardApi.deleteLabel(label.id);
    allBoardLabels.value = allBoardLabels.value.filter(
      (l) => l.id !== label.id,
    );
    if (detail.value)
      detail.value.labels = detail.value.labels.filter(
        (l) => l.id !== label.id,
      );
    emit("updated");
  } catch {
    $q.notify({ type: "negative", message: "Failed to delete label" });
  }
};

const saveCustomPreset = () => {
  const preset = { bg: customPreset.value.bg, text: customPreset.value.text };
  allPresets.value.push(preset);
  const saved = loadCustomPresets();
  saved.push(preset);
  localStorage.setItem(CUSTOM_PRESETS_KEY, JSON.stringify(saved));
  newLabel.value.bg = preset.bg;
  newLabel.value.text = preset.text;
  showCustomPicker.value = false;
};
// ─────────────────────────────────────────────────────────────────────────────

// ─── Type management state ────────────────────────────────────────────────────
const allBoardTypes = ref([]);
watch(
  () => props.boardTypes,
  (v) => {
    allBoardTypes.value = [...v];
  },
  { immediate: true },
);

const newType = ref({
  name: "",
  bg: LABEL_PRESETS[4].bg,
  text: LABEL_PRESETS[4].text,
});
const creatingType = ref(false);

const isTypeAttached = (type) =>
  detail.value?.types?.some((t) => t.id === type.id);

const toggleType = async (type) => {
  if (isTypeAttached(type)) await removeType(type);
  else await addType(type);
};

const createType = async () => {
  if (!newType.value.name.trim() || !props.boardId) return;
  creatingType.value = true;
  try {
    const res = await boardApi.createType(props.boardId, {
      name: newType.value.name.trim(),
      color: newType.value.bg,
      textColor: newType.value.text,
    });
    allBoardTypes.value.push(res.data.data);
    newType.value.name = "";
    emit("updated");
  } catch {
    $q.notify({ type: "negative", message: "Failed to create type" });
  } finally {
    creatingType.value = false;
  }
};

const deleteBoardType = async (type) => {
  try {
    await boardApi.deleteType(type.id);
    allBoardTypes.value = allBoardTypes.value.filter(
      (t) => t.id !== type.id,
    );
    if (detail.value)
      detail.value.types = detail.value.types.filter(
        (t) => t.id !== type.id,
      );
    emit("updated");
  } catch {
    $q.notify({ type: "negative", message: "Failed to delete type" });
  }
};
// ─────────────────────────────────────────────────────────────────────────────

const allBoardMemberUsers = computed(() =>
  props.boardMembers.map((m) => m.user).filter(Boolean),
);

const isMemberAttached = (user) =>
  detail.value?.members?.some((m) => m.id === user.id);

const toggleMember = async (user) => {
  if (isMemberAttached(user)) await removeMember(user);
  else await addMember(user);
};

// Users who can be @mentioned: card members + board admins (deduplicated)
const taggableUsers = computed(() => {
  const cardMembers = detail.value?.members || [];
  const admins = props.boardMembers
    .filter((m) => m.role === 'ADMIN' || m.user?.role === 'ADMIN')
    .map((m) => m.user);
  const map = new Map();
  for (const u of [...cardMembers, ...admins]) {
    if (u && !map.has(u.id)) map.set(u.id, u);
  }
  return [...map.values()];
});

watch(
  () => [props.modelValue, props.cardId],
  async ([open, id]) => {
    if (open && id) {
      detail.value = null;
      descExpanded.value = false;
      try {
        const res = await cardApi.getById(id);
        detail.value = res.data.data;
        originalStatus.value = detail.value.status;
        originalColumnId.value = detail.value.columnId;
        scheduleOverflowCheck();
        nextTick(setupDescResizeObserver);
      } catch {
        $q.notify({ type: "negative", message: "Failed to load card" });
      }
    }
  },
);

const refreshDetail = async () => {
  if (!detail.value) return;
  try {
    const res = await cardApi.getById(detail.value.id);
    detail.value = res.data.data;
    originalStatus.value = detail.value.status;
    originalColumnId.value = detail.value.columnId;
    emit("dismiss-update");
  } catch {
    /* silent */
  }
};

const confirmDeleteCard = () => {
  $q.dialog({
    title: 'Delete Card',
    message: `Delete "${detail.value?.name}"? This cannot be undone.`,
    cancel: true,
    persistent: true,
    dark: true,
    color: 'red-5',
  }).onOk(async () => {
    try {
      await cardApi.delete(detail.value.id);
      show.value = false;
      emit("deleted");
    } catch {
      $q.notify({ type: "negative", message: "Failed to delete card" });
    }
  });
};

const formatDate = (d) => (d ? new Date(d).toLocaleString() : "");

const saveField = async (field, value) => {
  if (!detail.value) return;
  try {
    await cardApi.update(detail.value.id, { [field]: value });
    if (field !== "description" && field !== "name") emit("updated");
  } catch {
    /* silent */
  }
};

const onCommented = (comment) => {
  if (comment.parentId) {
    // Find parent and add to its replies
    const parent = detail.value.comments.find((c) => c.id === comment.parentId);
    if (parent) {
      if (!parent.replies) parent.replies = [];
      parent.replies.push(comment);
    }
  } else {
    detail.value.comments.push(comment);
  }
  replyTo.value = null;
};

const deleteComment = async (c) => {
  try {
    await cardApi.deleteComment(detail.value.id, c.id);
    if (c.parentId) {
      // Remove from parent's replies
      const parent = detail.value.comments.find((p) => p.id === c.parentId);
      if (parent) {
        parent.replies = parent.replies.filter((r) => r.id !== c.id);
      }
    } else {
      detail.value.comments = detail.value.comments.filter((x) => x.id !== c.id);
    }
  } catch {
    $q.notify({ type: "negative", message: "Failed to delete comment" });
  }
};

const handleReaction = async ({ commentId, reactionType }) => {
  try {
    const res = await cardApi.toggleReaction(detail.value.id, commentId, { reactionType });
    const reactions = res.data.data;
    // Find the comment (could be top-level or reply) and update its reactions
    for (const c of detail.value.comments) {
      if (c.id === commentId) {
        c.reactions = reactions;
        break;
      }
      if (c.replies) {
        const reply = c.replies.find((r) => r.id === commentId);
        if (reply) {
          reply.reactions = reactions;
          break;
        }
      }
    }
  } catch {
    $q.notify({ type: "negative", message: "Failed to toggle reaction" });
  }
};

const uploadAttachment = async (file) => {
  if (!file) return;
  attachFile.value = null;
  uploadingFile.value = true;
  const fd = new FormData();
  fd.append("file", file);
  try {
    const res = await cardApi.uploadAttachment(detail.value.id, fd);
    detail.value.attachments.push(res.data.data);
  } catch {
    $q.notify({ type: "negative", message: "Upload failed" });
  }
  uploadingFile.value = false;
};

const uploadImages = async (files) => {
  if (!files?.length) return;
  imageFile.value = null;
  uploadingImages.value = true;
  for (const file of files) {
    const fd = new FormData();
    fd.append("file", file);
    try {
      const res = await cardApi.uploadAttachment(detail.value.id, fd);
      detail.value.attachments.push(res.data.data);
    } catch {
      $q.notify({ type: "negative", message: `Failed to upload ${file.name}` });
    }
  }
  uploadingImages.value = false;
};

const deleteAttachment = async (att) => {
  try {
    await cardApi.deleteAttachment(detail.value.id, att.id);
    detail.value.attachments = detail.value.attachments.filter(
      (a) => a.id !== att.id,
    );
  } catch {
    $q.notify({ type: "negative", message: "Failed" });
  }
};

const setCoverFromExisting = async (img) => {
  try {
    await cardApi.update(detail.value.id, { mainImageUrl: img.url });
    detail.value.mainImageUrl = img.url;
    emit("updated");
  } catch {
    $q.notify({ type: "negative", message: "Failed to set cover" });
  }
};

const removeCoverImage = async () => {
  try {
    await cardApi.update(detail.value.id, { mainImageUrl: null });
    detail.value.mainImageUrl = null;
    emit("updated");
  } catch {
    $q.notify({ type: "negative", message: "Failed to remove cover" });
  }
};

const onCoverImageSelected = async (e) => {
  const file = e.target.files?.[0];
  if (!file) return;
  uploadingCover.value = true;
  const fd = new FormData();
  fd.append("file", file);
  fd.append("name", file.name);
  try {
    const res = await cardApi.uploadAttachment(detail.value.id, fd);
    await cardApi.update(detail.value.id, { mainImageUrl: res.data.data.url });
    detail.value.mainImageUrl = res.data.data.url;
    detail.value.attachments.push(res.data.data);
    emit("updated");
  } catch {
    $q.notify({ type: "negative", message: "Upload failed" });
  }
  uploadingCover.value = false;
  e.target.value = "";
};

const addLabel = async (label) => {
  try {
    await cardApi.addLabel(detail.value.id, label.id);
    detail.value.labels.push(label);
    emit("updated");
  } catch {
    $q.notify({ type: "negative", message: "Failed" });
  }
};

const removeLabel = async (label) => {
  try {
    await cardApi.removeLabel(detail.value.id, label.id);
    detail.value.labels = detail.value.labels.filter((l) => l.id !== label.id);
    emit("updated");
  } catch {
    $q.notify({ type: "negative", message: "Failed" });
  }
};

const addType = async (type) => {
  try {
    await cardApi.addType(detail.value.id, type.id);
    detail.value.types.push(type);
    emit("updated");
  } catch {
    $q.notify({ type: "negative", message: "Failed" });
  }
};

const removeType = async (type) => {
  try {
    await cardApi.removeType(detail.value.id, type.id);
    detail.value.types = detail.value.types.filter((t) => t.id !== type.id);
    emit("updated");
  } catch {
    $q.notify({ type: "negative", message: "Failed" });
  }
};

const addMember = async (user) => {
  try {
    await cardApi.addMember(detail.value.id, user.id);
    detail.value.members.push(user);
    emit("updated");
  } catch {
    $q.notify({ type: "negative", message: "Failed" });
  }
};

const removeMember = async (user) => {
  try {
    await cardApi.removeMember(detail.value.id, user.id);
    detail.value.members = detail.value.members.filter((m) => m.id !== user.id);
    emit("updated");
  } catch {
    $q.notify({ type: "negative", message: "Failed" });
  }
};

const addLink = async () => {
  if (!newLink.value.url.trim()) return;
  addingLink.value = true;
  try {
    const res = await cardApi.addLink(detail.value.id, {
      url: newLink.value.url.trim(),
      title: newLink.value.title.trim() || null,
    });
    if (!detail.value.links) detail.value.links = [];
    detail.value.links.push(res.data.data);
    newLink.value = { url: '', title: '' };
  } catch {
    $q.notify({ type: "negative", message: "Failed to add link" });
  } finally {
    addingLink.value = false;
  }
};

const deleteLink = async (link) => {
  try {
    await cardApi.deleteLink(detail.value.id, link.id);
    detail.value.links = detail.value.links.filter((l) => l.id !== link.id);
  } catch {
    $q.notify({ type: "negative", message: "Failed to delete link" });
  }
};
</script>

<style scoped>
.sidebar-section {
  margin-bottom: 20px;
}
.sidebar-label {
  font-size: 0.75rem;
  color: #9e9e9e;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.comment-item {
  background: #1a1a1a;
  border-radius: 6px;
  padding: 10px;
}
.conflict-banner {
  background: rgba(255, 152, 0, 0.1);
  border-bottom: 1px solid rgba(255, 152, 0, 0.25);
  flex-shrink: 0;
}

/* Label color preset grid */
.label-preset-grid {
  display: grid;
  grid-template-columns: repeat(10, 20px);
  gap: 4px;
}
.label-preset-swatch {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  cursor: pointer;
  border: 2px solid transparent;
  transition:
    transform 0.1s,
    border-color 0.1s;
}
.label-preset-swatch:hover {
  transform: scale(1.2);
}
.label-preset-swatch.preset-selected {
  border-color: #fff;
  transform: scale(1.1);
}

/* Label row hover */
.label-row:hover {
  background: rgba(255, 255, 255, 0.05);
}

/* Image thumbnails grid */
.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 8px;
}
.image-thumb-wrap {
  position: relative;
}
.image-thumb {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  cursor: pointer;
  transition: opacity 0.15s, transform 0.15s;
}
.image-thumb:hover {
  opacity: 0.85;
  transform: scale(1.03);
}
.image-thumb-actions {
  position: absolute;
  top: 4px;
  right: 4px;
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.15s;
}
.image-thumb-actions .q-btn {
  background: rgba(0, 0, 0, 0.6);
}
.image-thumb-wrap:hover .image-thumb-actions {
  opacity: 1;
}
.image-thumb-name {
  font-size: 0.68rem;
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Upload indicator */
.upload-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(38, 166, 154, 0.08);
  border: 1px solid rgba(38, 166, 154, 0.2);
  border-radius: 4px;
  flex-wrap: wrap;
}
.upload-indicator .q-linear-progress {
  width: 100%;
}

/* Lightbox */
.lightbox-backdrop {
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.92);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: zoom-out;
}
.lightbox-img {
  max-width: 90vw;
  max-height: calc(90vh - 80px);
  object-fit: contain;
  border-radius: 6px;
  cursor: default;
}
.lightbox-toolbar {
  position: absolute;
  top: 16px;
  right: 16px;
  display: flex;
  gap: 8px;
}

/* Slideshow arrows */
.slideshow-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  background: rgba(0, 0, 0, 0.5) !important;
}
.slideshow-arrow-left {
  left: 16px;
}
.slideshow-arrow-right {
  right: 16px;
}
/* Thumbnail strip */
.lightbox-thumbstrip {
  position: absolute;
  bottom: 16px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 6px;
  padding: 6px 10px;
  background: rgba(0, 0, 0, 0.6);
  border-radius: 8px;
  max-width: 80vw;
  overflow-x: auto;
}
.lightbox-thumb {
  width: 48px;
  height: 48px;
  border-radius: 4px;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
  flex-shrink: 0;
  opacity: 0.5;
  transition: opacity 0.15s, border-color 0.15s;
}
.lightbox-thumb.active {
  border-color: #26a69a;
  opacity: 1;
}
.lightbox-thumb:hover {
  opacity: 1;
}
.lightbox-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* Description editor wrapper for image overlay */
.desc-editor-wrap {
  position: relative;
  transition: max-height 0.3s ease;
}
.desc-editor-wrap.desc-collapsed {
  max-height: 500px;
  overflow: hidden;
  cursor: pointer;
}
.desc-editor-wrap.desc-collapsed .desc-editor :deep(.q-editor__toolbar) {
  display: none;
}

/* Fade overlay at bottom of collapsed description */
.desc-fade-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 100px;
  background: linear-gradient(transparent 0%, #111 75%);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  cursor: pointer;
  z-index: 5;
}
.desc-expand-bar {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 8px 0;
  font-size: 0.85rem;
  font-weight: 600;
  color: #80cbc4;
  background: rgba(38, 166, 154, 0.12);
  border: 1px solid rgba(38, 166, 154, 0.25);
  border-radius: 6px;
  transition: background 0.15s, border-color 0.15s;
}
.desc-expand-bar:hover {
  background: rgba(38, 166, 154, 0.22);
  border-color: rgba(38, 166, 154, 0.45);
}

/* Collapse bar below expanded description */
.desc-collapse-bar {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 8px 0;
  font-size: 0.85rem;
  font-weight: 600;
  color: #80cbc4;
  background: rgba(38, 166, 154, 0.12);
  border: 1px solid rgba(38, 166, 154, 0.25);
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}
.desc-collapse-bar:hover {
  background: rgba(38, 166, 154, 0.22);
  border-color: rgba(38, 166, 154, 0.45);
}

/* Description editor embedded images */
.desc-editor :deep(.q-editor__content img) {
  display: block;
  max-width: 66%;
  height: auto;
  border-radius: 6px;
  margin: 8px 0;
  object-fit: contain;
}
.desc-editor :deep(.q-editor__content) {
  min-height: 120px;
  overflow-wrap: break-word;
}

/* Floating overlay on description images */
.desc-img-overlay {
  position: absolute;
  pointer-events: auto;
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  gap: 4px;
  padding: 6px;
  border-radius: 6px;
  background: rgba(0, 0, 0, 0.45);
  z-index: 10;
  transition: opacity 0.15s;
}
.desc-img-overlay .q-btn {
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
}

/* Cover image */
.cover-image-wrap {
  position: relative;
  width: 100%;
  height: 200px;
  background: #1a1a1a;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}
.cover-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
  background: #1a1a1a;
}
.cover-image-actions {
  position: absolute;
  bottom: 10px;
  right: 10px;
  display: flex;
  gap: 6px;
  opacity: 0;
  transition: opacity 0.15s;
}
.cover-image-wrap:hover .cover-image-actions {
  opacity: 1;
}
.cover-image-empty {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Cover image picker grid */
.cover-pick-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
}
.cover-pick-item {
  position: relative;
  cursor: pointer;
  border-radius: 4px;
  border: 2px solid transparent;
  overflow: hidden;
  transition: border-color 0.15s;
}
.cover-pick-item img {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  display: block;
  transition: opacity 0.15s;
}
.cover-pick-item:hover img {
  opacity: 0.75;
}
.cover-pick-item.cover-pick-active {
  border-color: #26a69a;
}
.cover-pick-check {
  position: absolute;
  bottom: 2px;
  right: 2px;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
}

/* Metadata section */
.metadata-section {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 8px;
  padding: 14px;
}

/* Link text truncation */
.link-truncate {
  display: block;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  max-width: 480px;
}
</style>
