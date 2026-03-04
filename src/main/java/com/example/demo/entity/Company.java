package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.trustify.trustify.enums.CompanyStatus;
import com.trustify.trustify.enums.SubsPlan;
import com.trustify.trustify.enums.VerifyStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Company implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 255)
    private String name;
    @Column(unique = true, nullable = false, length = 255)
    private String slug;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(name = "tagline", length = 500)
    private String tagline;
    // ===== CONTACT INFORMATION =====
    @Pattern(regexp = "^(https?://)?(www\\.)?[a-zA-Z0-9-]+(\\.[a-zA-Z]{2,})+(/.*)?$",
            message = "Invalid website URL")
    @Column(name = "website_url", length = 500)
    private String websiteUrl;
    @Email(message = "Invalid email format")
    @Column(name = "contact_email", nullable = false, unique = true, length = 255)
    private String contactEmail;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number")
    @Column(name = "contact_phone", length = 20)
    private String contactPhone;
    // ===== BUSINESS INFORMATION =====
    @Column(name = "industry", length = 100)
    private String industry;
    @Column(name = "company_size", length = 50)
    private String companySize;

    @Column(name = "founded_year")
    private Integer foundedYear;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "city", length = 100)
    private String city;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "annual_revenue", length = 50)
    private String annualRevenue;

    // ===== MEDIA =====
    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    // ===== SOCIAL MEDIA =====
    @Column(name = "facebook_url", length = 500)
    private String facebookUrl;
    @Column(name = "twitter_url", length = 500)
    private String twitterUrl;
    @Column(name = "linkedin_url", length = 500)
    private String linkedinUrl;
    @Column(name = "instagram_url", length = 500)
    private String instagramUrl;

    // ===== STATUS & VERIFICATION =====
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private CompanyStatus status = CompanyStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private VerifyStatus verifyStatus = VerifyStatus.NONE;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private SubsPlan planName = SubsPlan.NONE;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planStart;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planEnd;

    @Column(length = 200)
    private String fileVerificationUrl;



    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "is_claimed", nullable = false)
    @Builder.Default
    private Boolean isClaimed = false;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    // ===== STATISTICS =====

    @Column(name = "total_reviews", nullable = false)
    @Builder.Default
    private Integer totalReviews = 0;
    @Column(name = "average_rating", columnDefinition = "DECIMAL(3,2)", nullable = false)
    @Builder.Default
    private Double averageRating = 0.0;
    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Long viewCount = 0L;

    // ===== TIMESTAMPS =====

    @CreationTimestamp
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ===== RELATIONSHIPS =====

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore  // Tránh circular reference
    private List<Review> reviews = new ArrayList<>();

    @OneToOne(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private CompanyRating rating;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore  // Tránh circular reference
    private List<ReviewReply> replies = new ArrayList<>();

    @ManyToMany(mappedBy = "companies")
    @JsonIgnore  // Tránh circular reference
    private Set<Category> categories;

    // Thay thế phần @ManyToMany cũ bằng:
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<Subscription> subscriptions = new ArrayList<>();


    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore  // Tránh circular reference
    @Builder.Default
    private List<UserBusiness> userBusinesses = new ArrayList<>();

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore  // Tránh circular reference
    @Builder.Default
    private List<Notification> notifications = new ArrayList<>();

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore  // Tránh circular reference
    @Builder.Default

    private List<Product> products = new ArrayList<>();
}