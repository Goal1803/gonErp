package com.gonerp.notes.service;

import com.gonerp.common.OrgContext;
import com.gonerp.notes.dto.*;
import com.gonerp.notes.model.Note;
import com.gonerp.notes.model.NoteFavorite;
import com.gonerp.notes.model.NoteLink;
import com.gonerp.notes.model.NoteMember;
import com.gonerp.notes.model.enums.NotePermission;
import com.gonerp.notes.model.NoteProperty;
import com.gonerp.notes.repository.NoteFavoriteRepository;
import com.gonerp.notes.repository.NotePropertyRepository;
import com.gonerp.notes.repository.NoteValueRepository;
import com.gonerp.notes.repository.NoteViewRepository;
import com.gonerp.notes.repository.NoteLinkRepository;
import com.gonerp.notes.repository.NoteMemberRepository;
import com.gonerp.notes.repository.NoteRepository;
import com.gonerp.organization.model.Organization;
import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoteService {

    private static final String OWNER = "OWNER";
    private static final String EDIT = "EDIT";
    private static final String VIEW = "VIEW";

    private final NoteRepository noteRepository;
    private final NoteMemberRepository memberRepository;
    private final NoteLinkRepository linkRepository;
    private final NoteFavoriteRepository favoriteRepository;
    private final NotePropertyRepository propertyRepository;
    private final NoteViewRepository viewRepository;
    private final NoteValueRepository valueRepository;
    private final UserRepository userRepository;

    // ------------------------------------------------------------------ tree

    /** Sidebar tree. Notes whose parent is not visible are surfaced at the top level. */
    @Transactional(readOnly = true)
    public List<NoteSummaryResponse> getTree() {
        User me = OrgContext.getCurrentUser(userRepository);
        List<Note> notes = noteRepository.findTree(me.getId(), orgId(me));
        return buildTree(notes, me);
    }

    @Transactional(readOnly = true)
    public List<NoteSummaryResponse> getTemplates() {
        User me = OrgContext.getCurrentUser(userRepository);
        return noteRepository.findTemplates(me.getId(), orgId(me)).stream()
                .map(NoteSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NoteSummaryResponse> getTrash() {
        User me = OrgContext.getCurrentUser(userRepository);
        return noteRepository.findTrash(me.getId(), orgId(me)).stream()
                .map(NoteSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NoteSummaryResponse> getFavorites() {
        User me = OrgContext.getCurrentUser(userRepository);
        return favoriteRepository.findByUserId(me.getId()).stream()
                .map(f -> {
                    NoteSummaryResponse r = NoteSummaryResponse.from(f.getNote());
                    r.setFavorite(true);
                    return r;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NoteSummaryResponse> getRecent(int limit) {
        User me = OrgContext.getCurrentUser(userRepository);
        return noteRepository.findRecent(me.getId(), orgId(me), PageRequest.of(0, limit)).stream()
                .map(NoteSummaryResponse::from)
                .toList();
    }

    private List<NoteSummaryResponse> buildTree(List<Note> notes, User me) {
        if (notes.isEmpty()) {
            return List.of();
        }
        Set<Long> favIds = new HashSet<>(favoriteRepository.findNoteIdsByUserId(me.getId()));
        Set<Long> sharedNoteIds = memberRepository.findByNoteIds(
                        notes.stream().map(Note::getId).toList()).stream()
                .map(m -> m.getNote().getId())
                .collect(Collectors.toSet());

        Map<Long, NoteSummaryResponse> byId = new LinkedHashMap<>();
        for (Note n : notes) {
            NoteSummaryResponse r = NoteSummaryResponse.from(n);
            r.setFavorite(favIds.contains(n.getId()));
            r.setShared(sharedNoteIds.contains(n.getId()));
            r.setPermission(n.getOwner().getId().equals(me.getId()) ? OWNER : EDIT);
            byId.put(n.getId(), r);
        }
        List<NoteSummaryResponse> roots = new ArrayList<>();
        for (NoteSummaryResponse r : byId.values()) {
            NoteSummaryResponse parent = r.getParentId() == null ? null : byId.get(r.getParentId());
            if (parent == null) {
                roots.add(r);
            } else {
                parent.getChildren().add(r);
            }
        }
        return roots;
    }

    // ------------------------------------------------------------------ read

    @Transactional(readOnly = true)
    public NoteDetailResponse getById(Long id) {
        User me = OrgContext.getCurrentUser(userRepository);
        Note note = require(id);
        String permission = requirePermission(note, me, false);

        NoteDetailResponse r = NoteDetailResponse.from(note);
        r.setPermission(permission);
        r.setFavorite(favoriteRepository.findByNoteIdAndUserId(id, me.getId()).isPresent());
        r.setBreadcrumb(ancestorsOf(note).stream().map(NoteRefResponse::from).toList());
        r.setChildren(noteRepository.findByParentIdOrderByPositionAsc(id).stream()
                .filter(c -> !c.isArchived() && c.getRowOf() == null)
                .map(NoteRefResponse::from)
                .toList());
        r.setBacklinks(linkRepository.findBySourceOf(id).stream()
                .map(l -> NoteRefResponse.from(l.getSource()))
                .toList());
        r.setMembers(membersFor(note));
        return r;
    }

    @Transactional(readOnly = true)
    public List<NoteSearchResponse> search(String q, int limit) {
        if (q == null || q.isBlank()) {
            return List.of();
        }
        User me = OrgContext.getCurrentUser(userRepository);
        List<Note> hits = noteRepository.search(me.getId(), orgId(me), q.trim(),
                PageRequest.of(0, limit));
        return hits.stream()
                .map(n -> NoteSearchResponse.from(n,
                        NoteContentUtil.snippet(n.getPlainText(), q.trim(), 60),
                        breadcrumbLabel(n)))
                .toList();
    }

    /** Backing list for the "@" mention picker inside the editor. */
    @Transactional(readOnly = true)
    public List<NoteRefResponse> mentionCandidates(String q, int limit) {
        User me = OrgContext.getCurrentUser(userRepository);
        String term = q == null ? "" : q.trim();
        List<Note> hits = term.isEmpty()
                ? noteRepository.findRecent(me.getId(), orgId(me), PageRequest.of(0, limit))
                : noteRepository.searchTitles(me.getId(), orgId(me), term, PageRequest.of(0, limit));
        return hits.stream().map(NoteRefResponse::from).toList();
    }

    // ----------------------------------------------------------------- write

    @Transactional
    public NoteDetailResponse create(NoteRequest request) {
        User me = OrgContext.getCurrentUser(userRepository);
        Organization org = me.getOrganization();

        Note parent = null;
        if (request.getParentId() != null) {
            parent = require(request.getParentId());
            requirePermission(parent, me, true);
        }

        Note note = Note.builder()
                .title(blankToDefault(request.getTitle()))
                .icon(request.getIcon())
                .coverColor(request.getCoverColor())
                .parent(parent)
                .owner(me)
                .organization(org)
                .template(Boolean.TRUE.equals(request.getTemplate()))
                .position(noteRepository.maxPosition(
                        parent == null ? null : parent.getId(), me.getId()) + 1)
                .build();

        if (request.getFromTemplateId() != null) {
            Note tpl = require(request.getFromTemplateId());
            requirePermission(tpl, me, false);
            note.setContent(tpl.getContent());
            note.setIcon(note.getIcon() != null ? note.getIcon() : tpl.getIcon());
            note.setCoverColor(note.getCoverColor() != null ? note.getCoverColor() : tpl.getCoverColor());
            if (isBlank(request.getTitle())) {
                note.setTitle(tpl.getTitle());
            }
        } else {
            note.setContent(request.getContent());
        }
        note.setPlainText(NoteContentUtil.extractPlainText(note.getContent()));

        noteRepository.save(note);
        note.setPath(pathFor(parent, note.getId()));
        noteRepository.save(note);
        syncLinks(note);

        NoteDetailResponse r = NoteDetailResponse.from(note);
        r.setPermission(OWNER);
        return r;
    }

    @Transactional
    public NoteDetailResponse update(Long id, NoteRequest request) {
        User me = OrgContext.getCurrentUser(userRepository);
        Note note = require(id);
        String permission = requirePermission(note, me, true);

        if (request.getTitle() != null) {
            note.setTitle(blankToDefault(request.getTitle()));
        }
        if (request.getIcon() != null) {
            note.setIcon(request.getIcon().isBlank() ? null : request.getIcon());
        }
        if (request.getCoverColor() != null) {
            note.setCoverColor(request.getCoverColor().isBlank() ? null : request.getCoverColor());
        }
        if (request.getContent() != null) {
            note.setContent(request.getContent());
            note.setPlainText(NoteContentUtil.extractPlainText(request.getContent()));
            syncLinks(note);
        }
        if (request.getFullWidth() != null) {
            note.setFullWidth(request.getFullWidth());
        }
        if (request.getTemplate() != null) {
            note.setTemplate(request.getTemplate());
        }
        // Only the owner decides whether a note is locked, otherwise an editor
        // could simply unlock what the owner froze.
        if (request.getLocked() != null && OWNER.equals(permission)) {
            note.setLocked(request.getLocked());
        }
        noteRepository.save(note);

        NoteDetailResponse r = NoteDetailResponse.from(note);
        r.setPermission(permission);
        return r;
    }

    @Transactional
    public void move(Long id, NoteMoveRequest request) {
        User me = OrgContext.getCurrentUser(userRepository);
        Note note = require(id);
        requirePermission(note, me, true);

        Note newParent = null;
        if (request.getParentId() != null) {
            newParent = require(request.getParentId());
            requirePermission(newParent, me, true);
            if (newParent.getId().equals(note.getId())
                    || isDescendant(newParent, note.getId())) {
                throw new IllegalArgumentException("Cannot move a note inside itself");
            }
        }

        Long oldParentId = note.getParent() == null ? null : note.getParent().getId();
        note.setParent(newParent);
        noteRepository.save(note);
        repath(note);

        Long newParentId = newParent == null ? null : newParent.getId();
        reorderSiblings(newParentId, note, request.getPosition());
        if (!Objects.equals(oldParentId, newParentId)) {
            normalisePositions(siblingsOf(oldParentId, note.getOwner().getId(), null));
        }
    }

    @Transactional
    public NoteDetailResponse duplicate(Long id) {
        User me = OrgContext.getCurrentUser(userRepository);
        Note source = require(id);
        requirePermission(source, me, false);
        Note copy = copyRecursive(source, source.getParent(), me, " (copy)");
        NoteDetailResponse r = NoteDetailResponse.from(copy);
        r.setPermission(OWNER);
        return r;
    }

    private Note copyRecursive(Note source, Note parent, User me, String titleSuffix) {
        Note copy = Note.builder()
                .title(source.getTitle() + titleSuffix)
                .icon(source.getIcon())
                .coverColor(source.getCoverColor())
                .content(source.getContent())
                .plainText(source.getPlainText())
                .parent(parent)
                .owner(me)
                .organization(me.getOrganization())
                .template(source.isTemplate())
                .fullWidth(source.isFullWidth())
                .position(noteRepository.maxPosition(
                        parent == null ? null : parent.getId(), me.getId()) + 1)
                .build();
        noteRepository.save(copy);
        copy.setPath(pathFor(parent, copy.getId()));
        noteRepository.save(copy);
        syncLinks(copy);

        for (Note child : noteRepository.findByParentIdOrderByPositionAsc(source.getId())) {
            if (!child.isArchived()) {
                copyRecursive(child, copy, me, "");
            }
        }
        return copy;
    }

    /** Soft-delete: the note and its whole sub-tree go to the trash together. */
    @Transactional
    public void archive(Long id) {
        User me = OrgContext.getCurrentUser(userRepository);
        Note note = require(id);
        requirePermission(note, me, true);
        for (Note n : noteRepository.findSubtree(id)) {
            n.setArchived(true);
            n.setArchivedRoot(n.getId().equals(id));
            noteRepository.save(n);
        }
    }

    @Transactional
    public void restore(Long id) {
        User me = OrgContext.getCurrentUser(userRepository);
        Note note = require(id);
        requirePermission(note, me, true);
        // A note whose parent is still in the trash comes back at the top level,
        // otherwise it would restore into something the user cannot see.
        if (note.getParent() != null && note.getParent().isArchived()) {
            note.setParent(null);
            noteRepository.save(note);
            repath(note);
        }
        for (Note n : noteRepository.findSubtree(id)) {
            n.setArchived(false);
            n.setArchivedRoot(false);
            noteRepository.save(n);
        }
    }

    @Transactional
    public void deleteForever(Long id) {
        User me = OrgContext.getCurrentUser(userRepository);
        Note note = require(id);
        if (!note.getOwner().getId().equals(me.getId()) && !OrgContext.isSuperAdmin()) {
            throw new AccessDeniedException("Only the owner can permanently delete this note");
        }
        List<Note> subtree = noteRepository.findSubtree(id);
        // Deepest first so no row is removed while it is still somebody's parent.
        subtree.sort(Comparator.comparingInt((Note n) -> depthOf(n)).reversed());
        for (Note n : subtree) {
            linkRepository.deleteBySourceId(n.getId());
            linkRepository.deleteByTargetId(n.getId());
            favoriteRepository.deleteByNoteId(n.getId());
            memberRepository.deleteByNoteId(n.getId());
            // Cells first (this note may be a row), then the schema if it is a database.
            valueRepository.deleteByRowId(n.getId());
            if (n.isDatabase()) {
                for (NoteProperty p : propertyRepository.findByDatabaseIdOrderByPositionAsc(n.getId())) {
                    valueRepository.deleteByPropertyId(p.getId());
                }
                propertyRepository.deleteByDatabaseId(n.getId());
                viewRepository.deleteByDatabaseId(n.getId());
            }
            noteRepository.delete(n);
        }
    }

    @Transactional
    public void emptyTrash() {
        User me = OrgContext.getCurrentUser(userRepository);
        for (Note n : noteRepository.findTrash(me.getId(), orgId(me))) {
            if (n.getOwner().getId().equals(me.getId())) {
                deleteForever(n.getId());
            }
        }
    }

    // ------------------------------------------------------------- favorites

    @Transactional
    public boolean toggleFavorite(Long id) {
        User me = OrgContext.getCurrentUser(userRepository);
        Note note = require(id);
        requirePermission(note, me, false);
        Optional<NoteFavorite> existing = favoriteRepository.findByNoteIdAndUserId(id, me.getId());
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            return false;
        }
        favoriteRepository.save(NoteFavorite.builder()
                .note(note)
                .user(me)
                .position(favoriteRepository.findByUserId(me.getId()).size())
                .build());
        return true;
    }

    // ----------------------------------------------------------------- share

    @Transactional(readOnly = true)
    public List<NoteMemberResponse> getMembers(Long id) {
        User me = OrgContext.getCurrentUser(userRepository);
        Note note = require(id);
        requirePermission(note, me, false);
        return membersFor(note);
    }

    @Transactional
    public NoteMemberResponse addMember(Long id, NoteMemberRequest request) {
        User me = OrgContext.getCurrentUser(userRepository);
        Note note = require(id);
        requireOwner(note, me);

        User target = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (target.getId().equals(note.getOwner().getId())) {
            throw new IllegalArgumentException("The owner already has full access");
        }
        NoteMember member = memberRepository.findByNoteIdAndUserId(id, target.getId())
                .orElseGet(() -> NoteMember.builder().note(note).user(target).build());
        member.setPermission(request.getPermission() == null
                ? NotePermission.EDIT : request.getPermission());
        memberRepository.save(member);
        return NoteMemberResponse.from(member, false);
    }

    @Transactional
    public void removeMember(Long id, Long userId) {
        User me = OrgContext.getCurrentUser(userRepository);
        Note note = require(id);
        requireOwner(note, me);
        memberRepository.deleteByNoteIdAndUserId(id, userId);
    }

    /** Shares placed on this note plus the ones inherited from its ancestors. */
    private List<NoteMemberResponse> membersFor(Note note) {
        List<Long> chain = new ArrayList<>(idsInPath(note));
        List<NoteMemberResponse> out = new ArrayList<>();
        Set<Long> seenUsers = new HashSet<>();
        // Walk from the note upwards so the closest share wins for a given user.
        Collections.reverse(chain);
        for (Long noteId : chain) {
            for (NoteMember m : memberRepository.findByNoteId(noteId)) {
                if (seenUsers.add(m.getUser().getId())) {
                    out.add(NoteMemberResponse.from(m, !noteId.equals(note.getId())));
                }
            }
        }
        return out;
    }

    // ------------------------------------------------------------ backlinks

    /** Rewrites this note's outgoing links to match what the document now mentions. */
    private void syncLinks(Note note) {
        Set<Long> wanted = NoteContentUtil.extractMentionedNoteIds(note.getContent());
        wanted.remove(note.getId());
        Map<Long, NoteLink> existing = linkRepository.findOutgoing(note.getId()).stream()
                .collect(Collectors.toMap(l -> l.getTarget().getId(), l -> l, (a, b) -> a));

        for (Map.Entry<Long, NoteLink> e : existing.entrySet()) {
            if (!wanted.contains(e.getKey())) {
                linkRepository.delete(e.getValue());
            }
        }
        for (Long targetId : wanted) {
            if (!existing.containsKey(targetId)) {
                noteRepository.findById(targetId).ifPresent(target ->
                        linkRepository.save(NoteLink.builder().source(note).target(target).build()));
            }
        }
    }

    // ----------------------------------------------------------- permissions

    /** Access check other note services can reuse. Returns OWNER / EDIT / VIEW. */
    public String requireAccess(Long noteId, boolean write) {
        return requirePermission(require(noteId), OrgContext.getCurrentUser(userRepository), write);
    }

    public Note requireNote(Long noteId) {
        return require(noteId);
    }

    /**
     * @param write whether the caller wants to modify the note
     * @return OWNER / EDIT / VIEW
     */
    private String requirePermission(Note note, User me, boolean write) {
        String level = permissionOf(note, me);
        if (level == null) {
            throw new AccessDeniedException("You do not have access to this note");
        }
        if (write && VIEW.equals(level)) {
            throw new AccessDeniedException("You only have view access to this note");
        }
        if (write && note.isLocked() && !OWNER.equals(level)) {
            throw new AccessDeniedException("This note is locked");
        }
        return level;
    }

    private void requireOwner(Note note, User me) {
        if (!note.getOwner().getId().equals(me.getId()) && !OrgContext.isSuperAdmin()) {
            throw new AccessDeniedException("Only the owner can manage sharing");
        }
    }

    /** Null when the user has no access at all. */
    private String permissionOf(Note note, User me) {
        if (note.getOwner().getId().equals(me.getId())) {
            return OWNER;
        }
        if (OrgContext.isSuperAdmin()) {
            return EDIT;
        }
        // A share on any ancestor covers this note; the most permissive one wins.
        List<Long> chain = idsInPath(note);
        if (chain.isEmpty()) {
            return null;
        }
        List<NoteMember> shares = memberRepository.findByNoteIds(chain);
        String best = null;
        for (NoteMember m : shares) {
            if (!m.getUser().getId().equals(me.getId())) {
                continue;
            }
            if (m.getPermission() == NotePermission.EDIT) {
                return EDIT;
            }
            best = VIEW;
        }
        return best;
    }

    // ------------------------------------------------------------- tree util

    /** Ids of every ancestor plus the note itself, root first. */
    private List<Long> idsInPath(Note note) {
        String path = note.getPath();
        if (path == null || path.isBlank()) {
            return List.of(note.getId());
        }
        List<Long> ids = new ArrayList<>();
        for (String part : path.split("/")) {
            if (!part.isBlank()) {
                try {
                    ids.add(Long.parseLong(part));
                } catch (NumberFormatException ignored) {
                    // Corrupt path segment — skip rather than deny access outright.
                }
            }
        }
        return ids.isEmpty() ? List.of(note.getId()) : ids;
    }

    private List<Note> ancestorsOf(Note note) {
        List<Long> ids = new ArrayList<>(idsInPath(note));
        ids.remove(note.getId());
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, Note> found = noteRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Note::getId, n -> n));
        return ids.stream().map(found::get).filter(Objects::nonNull).toList();
    }

    private String breadcrumbLabel(Note note) {
        List<Note> chain = ancestorsOf(note);
        return chain.isEmpty() ? ""
                : chain.stream().map(Note::getTitle).collect(Collectors.joining(" / "));
    }

    private static String pathFor(Note parent, Long id) {
        String prefix = parent == null || parent.getPath() == null || parent.getPath().isBlank()
                ? "/" : parent.getPath();
        return prefix + id + "/";
    }

    /** Rewrites the path of a note and everything nested under it after a move. */
    private void repath(Note note) {
        String oldPath = note.getPath();
        String newPath = pathFor(note.getParent(), note.getId());
        note.setPath(newPath);
        noteRepository.save(note);
        if (oldPath == null || oldPath.equals(newPath)) {
            return;
        }
        for (Note child : noteRepository.findSubtree(note.getId())) {
            if (child.getId().equals(note.getId()) || child.getPath() == null) {
                continue;
            }
            child.setPath(newPath + child.getPath().substring(oldPath.length()));
            noteRepository.save(child);
        }
    }

    private boolean isDescendant(Note candidate, Long ancestorId) {
        return candidate.getPath() != null && candidate.getPath().contains("/" + ancestorId + "/");
    }

    private int depthOf(Note n) {
        return n.getPath() == null ? 0 : (int) n.getPath().chars().filter(c -> c == '/').count();
    }

    private List<Note> siblingsOf(Long parentId, Long ownerId, Long excludeId) {
        List<Note> siblings = parentId == null
                ? new ArrayList<>(noteRepository.findTopLevelByOwner(ownerId))
                : new ArrayList<>(noteRepository.findByParentIdOrderByPositionAsc(parentId));
        siblings.removeIf(n -> n.isArchived() || (excludeId != null && n.getId().equals(excludeId)));
        return siblings;
    }

    private void reorderSiblings(Long parentId, Note moved, Integer position) {
        List<Note> siblings = siblingsOf(parentId, moved.getOwner().getId(), moved.getId());
        int index = position == null || position < 0 || position > siblings.size()
                ? siblings.size() : position;
        siblings.add(index, moved);
        normalisePositions(siblings);
    }

    private void normalisePositions(List<Note> ordered) {
        for (int i = 0; i < ordered.size(); i++) {
            Note n = ordered.get(i);
            if (n.getPosition() != i) {
                n.setPosition(i);
                noteRepository.save(n);
            }
        }
    }

    // ---------------------------------------------------------------- shared

    private Note require(Long id) {
        return noteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Note not found: " + id));
    }

    private Long orgId(User user) {
        return user.getOrganization() == null ? null : user.getOrganization().getId();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String blankToDefault(String title) {
        return isBlank(title) ? "Untitled" : title.trim();
    }
}
