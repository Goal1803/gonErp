package com.gonerp.taskmanager.service;

import com.gonerp.taskmanager.dto.DesignDashboardResponse;
import com.gonerp.taskmanager.model.Board;
import com.gonerp.taskmanager.model.BoardMember;
import com.gonerp.taskmanager.model.Card;
import com.gonerp.taskmanager.model.DesignDetail;
import com.gonerp.taskmanager.model.enums.BoardType;
import com.gonerp.taskmanager.repository.*;
import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.repository.UserRepository;
import com.gonerp.worktime.model.WorkTimeSettings;
import com.gonerp.worktime.repository.WorkTimeSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DesignDashboardService {

    private final BoardRepository boardRepository;
    private final CardRepository cardRepository;
    private final CardActivityRepository cardActivityRepository;
    private final DesignDetailRepository designDetailRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final WorkTimeSettingsRepository workTimeSettingsRepository;
    private final UserRepository userRepository;

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Berlin");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public DesignDashboardResponse getDashboard(Long boardId, LocalDate startDate, LocalDate endDate, Long filterUserId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("Board not found: " + boardId));

        if (board.getBoardType() != BoardType.POD_DESIGN) {
            throw new IllegalArgumentException("Board is not a POD_DESIGN board");
        }

        Long orgId = board.getOrganization() != null ? board.getOrganization().getId() : null;
        ZoneId zoneId = resolveTimezone(orgId);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        return buildDashboard(List.of(boardId), startDateTime, endDateTime, filterUserId, startDate, endDate, zoneId);
    }

    public DesignDashboardResponse getCombinedDashboard(LocalDate startDate, LocalDate endDate, Long filterUserId, Long orgId) {
        List<Board> boards = boardRepository.findByOrganizationIdAndBoardType(orgId, BoardType.POD_DESIGN);

        if (boards.isEmpty()) {
            return buildEmptyDashboard(startDate, endDate);
        }

        ZoneId zoneId = resolveTimezone(orgId);
        List<Long> boardIds = boards.stream().map(Board::getId).toList();

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        return buildDashboard(boardIds, startDateTime, endDateTime, filterUserId, startDate, endDate, zoneId);
    }

    private DesignDashboardResponse buildDashboard(List<Long> boardIds, LocalDateTime startDateTime,
                                                    LocalDateTime endDateTime, Long filterUserId,
                                                    LocalDate startDate, LocalDate endDate, ZoneId zoneId) {
        boolean singleBoard = boardIds.size() == 1;
        Long boardId = singleBoard ? boardIds.get(0) : null;

        // Get cards filtered by user involvement (member/designer/creator)
        // When user is a CardMember, they're involved with that card (creators and designers are auto-added as members)
        List<Card> userCards;
        if (filterUserId != null) {
            userCards = singleBoard
                    ? cardRepository.findByMemberInBoardBetween(boardId, filterUserId, startDateTime, endDateTime)
                    : cardRepository.findByMemberInBoardsBetween(boardIds, filterUserId, startDateTime, endDateTime);
        } else {
            userCards = singleBoard
                    ? cardRepository.findCreatedInBoardBetween(boardId, startDateTime, endDateTime)
                    : cardRepository.findCreatedInBoardsBetween(boardIds, startDateTime, endDateTime);
        }
        Set<Long> userCardIds = userCards.stream().map(Card::getId).collect(Collectors.toSet());

        // Exclude cards in "Draft" column from totalCreated
        List<Card> nonDraftCards = userCards.stream()
                .filter(c -> c.getColumn() == null || !"Draft".equalsIgnoreCase(c.getColumn().getTitle()))
                .toList();
        int totalCreated = nonDraftCards.size();

        // Cancelled designs (cards in "Canceled" column)
        int totalCancelled = (int) userCards.stream()
                .filter(c -> c.getColumn() != null && "Canceled".equalsIgnoreCase(c.getColumn().getTitle()))
                .count();

        // Completed designs
        List<DesignDetail> completedDesigns = singleBoard
                ? designDetailRepository.findCompletedInBoardBetween(boardId, startDateTime, endDateTime)
                : designDetailRepository.findCompletedInBoardsBetween(boardIds, startDateTime, endDateTime);

        if (filterUserId != null) {
            completedDesigns = completedDesigns.stream()
                    .filter(dd -> dd.getCard() != null && userCardIds.contains(dd.getCard().getId()))
                    .toList();
        }

        int totalCompleted = completedDesigns.size();
        double completionRate = totalCreated > 0 ? (double) totalCompleted / totalCreated * 100 : 0;

        // Average hours to complete
        double avgHoursToComplete = calculateAvgHoursToComplete(completedDesigns);

        // Total rejected
        int totalRejected = singleBoard
                ? (int) cardActivityRepository.countRejectedInBoardBetween(boardId, startDateTime, endDateTime)
                : (int) cardActivityRepository.countRejectedInBoardsBetween(boardIds, startDateTime, endDateTime);

        // Designs by stage (snapshot - shows current column of user's cards)
        Map<String, Integer> designsByStage = new LinkedHashMap<>();
        if (filterUserId != null) {
            // Get ALL cards for this user (not just in date range) for the stage snapshot
            List<Card> allUserCards = singleBoard
                    ? cardRepository.findByMemberInBoardBetween(boardId, filterUserId,
                        LocalDateTime.of(2000, 1, 1, 0, 0), LocalDateTime.of(2100, 1, 1, 0, 0))
                    : cardRepository.findByMemberInBoardsBetween(boardIds, filterUserId,
                        LocalDateTime.of(2000, 1, 1, 0, 0), LocalDateTime.of(2100, 1, 1, 0, 0));
            for (Card c : allUserCards) {
                String stage = c.getColumn() != null ? c.getColumn().getTitle() : "Unknown";
                designsByStage.merge(stage, 1, Integer::sum);
            }
        } else {
            List<Object[]> stageCounts = singleBoard
                    ? cardRepository.countByColumnForBoard(boardId)
                    : cardRepository.countByColumnForBoards(boardIds);
            for (Object[] row : stageCounts) {
                designsByStage.put((String) row[0], ((Long) row[1]).intValue());
            }
        }

        // Member stats
        List<DesignDashboardResponse.MemberStats> memberStats = buildMemberStats(
                boardIds, singleBoard, boardId, startDateTime, endDateTime, filterUserId);

        // Idea Creator stats - count designs where each user is the ideaCreator
        List<DesignDashboardResponse.MemberStats> ideaCreatorStats = buildRoleStats(
                userCards, completedDesigns, "IDEA_CREATOR", filterUserId);

        // Designer stats - count designs where each user is a designer
        List<DesignDashboardResponse.MemberStats> designerStats = buildRoleStats(
                userCards, completedDesigns, "DESIGNER", filterUserId);

        // Product type stats
        List<DesignDashboardResponse.ProductTypeStats> productTypeStats = new ArrayList<>();
        List<Object[]> ptCounts = singleBoard
                ? designDetailRepository.countProductTypesInBoardBetween(boardId, startDateTime, endDateTime)
                : designDetailRepository.countProductTypesInBoardsBetween(boardIds, startDateTime, endDateTime);
        for (Object[] row : ptCounts) {
            productTypeStats.add(DesignDashboardResponse.ProductTypeStats.builder()
                    .name((String) row[0])
                    .count(((Long) row[1]).intValue())
                    .build());
        }

        // Niche stats
        List<DesignDashboardResponse.NicheStats> nicheStats = new ArrayList<>();
        List<Object[]> nicheCounts = singleBoard
                ? designDetailRepository.countNichesInBoardBetween(boardId, startDateTime, endDateTime)
                : designDetailRepository.countNichesInBoardsBetween(boardIds, startDateTime, endDateTime);
        for (Object[] row : nicheCounts) {
            nicheStats.add(DesignDashboardResponse.NicheStats.builder()
                    .name((String) row[0])
                    .count(((Long) row[1]).intValue())
                    .build());
        }

        // Daily trends
        List<DesignDashboardResponse.DailyTrend> dailyTrends = buildDailyTrends(
                boardIds, singleBoard, boardId, startDate, endDate, filterUserId, completedDesigns);

        return DesignDashboardResponse.builder()
                .totalCreated(totalCreated)
                .totalCompleted(totalCompleted)
                .totalCancelled(totalCancelled)
                .completionRate(Math.round(completionRate * 100.0) / 100.0)
                .avgHoursToComplete(Math.round(avgHoursToComplete * 100.0) / 100.0)
                .totalRejected(totalRejected)
                .designsByStage(designsByStage)
                .memberStats(memberStats)
                .ideaCreatorStats(ideaCreatorStats)
                .designerStats(designerStats)
                .productTypeStats(productTypeStats)
                .nicheStats(nicheStats)
                .dailyTrends(dailyTrends)
                .build();
    }

    private List<DesignDashboardResponse.MemberStats> buildRoleStats(
            List<Card> allCards, List<DesignDetail> completedDesigns,
            String role, Long filterUserId) {

        // Get all design details for the cards
        Map<Long, User> userMap = new LinkedHashMap<>();

        for (Card card : allCards) {
            DesignDetail dd = designDetailRepository.findByCardId(card.getId()).orElse(null);
            if (dd == null) continue;

            if ("IDEA_CREATOR".equals(role) && dd.getIdeaCreator() != null) {
                userMap.putIfAbsent(dd.getIdeaCreator().getId(), dd.getIdeaCreator());
            } else if ("DESIGNER".equals(role) && dd.getDesigners() != null) {
                for (User designer : dd.getDesigners()) {
                    userMap.putIfAbsent(designer.getId(), designer);
                }
            }
        }

        if (filterUserId != null) {
            userMap.keySet().retainAll(Set.of(filterUserId));
        }

        Set<Long> completedCardIds = completedDesigns.stream()
                .filter(dd -> dd.getCard() != null)
                .map(dd -> dd.getCard().getId())
                .collect(Collectors.toSet());

        List<DesignDashboardResponse.MemberStats> stats = new ArrayList<>();
        for (Map.Entry<Long, User> entry : userMap.entrySet()) {
            User user = entry.getValue();
            int created = 0;
            int completed = 0;

            for (Card card : allCards) {
                DesignDetail dd = designDetailRepository.findByCardId(card.getId()).orElse(null);
                if (dd == null) continue;

                boolean involved = false;
                if ("IDEA_CREATOR".equals(role) && dd.getIdeaCreator() != null
                        && dd.getIdeaCreator().getId().equals(user.getId())) {
                    involved = true;
                } else if ("DESIGNER".equals(role) && dd.getDesigners() != null
                        && dd.getDesigners().stream().anyMatch(d -> d.getId().equals(user.getId()))) {
                    involved = true;
                }

                if (involved) {
                    created++;
                    if (completedCardIds.contains(card.getId())) {
                        completed++;
                    }
                }
            }

            if (created > 0 || completed > 0) {
                stats.add(DesignDashboardResponse.MemberStats.builder()
                        .userId(user.getId())
                        .userName(user.getUserName())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .avatarUrl(user.getAvatarUrl())
                        .created(created)
                        .completed(completed)
                        .avgHoursToComplete(0)
                        .build());
            }
        }

        stats.sort((a, b) -> b.getCompleted() - a.getCompleted());
        return stats;
    }

    private double calculateAvgHoursToComplete(List<DesignDetail> completedDesigns) {
        if (completedDesigns.isEmpty()) return 0;

        double totalHours = 0;
        int count = 0;
        for (DesignDetail dd : completedDesigns) {
            if (dd.getCard() != null && dd.getCard().getCreatedAt() != null && dd.getApprovalDate() != null) {
                long hours = ChronoUnit.HOURS.between(dd.getCard().getCreatedAt(), dd.getApprovalDate());
                totalHours += hours;
                count++;
            }
        }
        return count > 0 ? totalHours / count : 0;
    }

    private List<DesignDashboardResponse.MemberStats> buildMemberStats(
            List<Long> boardIds, boolean singleBoard, Long boardId,
            LocalDateTime startDateTime, LocalDateTime endDateTime, Long filterUserId) {

        // Collect unique members from all boards
        Set<Long> memberUserIds = new HashSet<>();
        for (Long bid : boardIds) {
            Board board = boardRepository.findById(bid).orElse(null);
            if (board != null) {
                for (BoardMember bm : board.getMembers()) {
                    memberUserIds.add(bm.getUser().getId());
                }
                memberUserIds.add(board.getOwner().getId());
            }
        }

        if (filterUserId != null) {
            memberUserIds = Set.of(filterUserId);
        }

        List<DesignDashboardResponse.MemberStats> stats = new ArrayList<>();
        for (Long userId : memberUserIds) {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) continue;

            String username = user.getUserName();

            // Created count
            int created = singleBoard
                    ? (int) cardRepository.countCreatedInBoardByUserBetween(boardId, username, startDateTime, endDateTime)
                    : (int) cardRepository.countCreatedInBoardsByUserBetween(boardIds, username, startDateTime, endDateTime);

            // Completed count
            List<DesignDetail> userCompleted = singleBoard
                    ? designDetailRepository.findCompletedByDesignerInBoardBetween(boardId, userId, startDateTime, endDateTime)
                    : designDetailRepository.findCompletedByDesignerInBoardsBetween(boardIds, userId, startDateTime, endDateTime);
            int completed = userCompleted.size();

            double avgDays = calculateAvgHoursToComplete(userCompleted);

            if (created > 0 || completed > 0) {
                stats.add(DesignDashboardResponse.MemberStats.builder()
                        .userId(userId)
                        .userName(username)
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .avatarUrl(user.getAvatarUrl())
                        .created(created)
                        .completed(completed)
                        .avgHoursToComplete(Math.round(avgDays * 100.0) / 100.0)
                        .build());
            }
        }

        // Sort by completed desc, then created desc
        stats.sort((a, b) -> {
            int cmp = Integer.compare(b.getCompleted(), a.getCompleted());
            return cmp != 0 ? cmp : Integer.compare(b.getCreated(), a.getCreated());
        });

        return stats;
    }

    private List<DesignDashboardResponse.DailyTrend> buildDailyTrends(
            List<Long> boardIds, boolean singleBoard, Long boardId,
            LocalDate startDate, LocalDate endDate, Long filterUserId,
            List<DesignDetail> completedDesigns) {

        // Get all created cards in range for daily grouping
        List<Card> createdCards = singleBoard
                ? cardRepository.findCreatedInBoardBetween(boardId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay())
                : cardRepository.findCreatedInBoardsBetween(boardIds, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());

        if (filterUserId != null) {
            User filterUser = userRepository.findById(filterUserId).orElse(null);
            if (filterUser != null) {
                String username = filterUser.getUserName();
                createdCards = createdCards.stream()
                        .filter(c -> username.equals(c.getCreatedBy()))
                        .toList();
            }
        }

        // Group created cards by date
        Map<String, Integer> createdByDate = createdCards.stream()
                .filter(c -> c.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        c -> c.getCreatedAt().toLocalDate().format(DATE_FORMAT),
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

        // Group completed designs by approval date
        Map<String, Integer> completedByDate = completedDesigns.stream()
                .filter(dd -> dd.getApprovalDate() != null)
                .collect(Collectors.groupingBy(
                        dd -> dd.getApprovalDate().toLocalDate().format(DATE_FORMAT),
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

        // Build daily trends for each day in range
        List<DesignDashboardResponse.DailyTrend> trends = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            String dateStr = current.format(DATE_FORMAT);
            trends.add(DesignDashboardResponse.DailyTrend.builder()
                    .date(dateStr)
                    .created(createdByDate.getOrDefault(dateStr, 0))
                    .completed(completedByDate.getOrDefault(dateStr, 0))
                    .build());
            current = current.plusDays(1);
        }

        return trends;
    }

    private DesignDashboardResponse buildEmptyDashboard(LocalDate startDate, LocalDate endDate) {
        List<DesignDashboardResponse.DailyTrend> emptyTrends = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            emptyTrends.add(DesignDashboardResponse.DailyTrend.builder()
                    .date(current.format(DATE_FORMAT))
                    .created(0)
                    .completed(0)
                    .build());
            current = current.plusDays(1);
        }

        return DesignDashboardResponse.builder()
                .totalCreated(0)
                .totalCompleted(0)
                .completionRate(0)
                .avgHoursToComplete(0)
                .totalRejected(0)
                .designsByStage(new LinkedHashMap<>())
                .memberStats(new ArrayList<>())
                .productTypeStats(new ArrayList<>())
                .nicheStats(new ArrayList<>())
                .dailyTrends(emptyTrends)
                .build();
    }

    private ZoneId resolveTimezone(Long orgId) {
        if (orgId == null) return DEFAULT_ZONE;
        return workTimeSettingsRepository.findByOrganizationId(orgId)
                .map(WorkTimeSettings::getZoneId)
                .orElse(DEFAULT_ZONE);
    }
}
