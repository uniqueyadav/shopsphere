package com.shopsphere.category.entity;

import com.shopsphere.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(
		name = "categories",
		uniqueConstraints = {
				@UniqueConstraint(name = "uk_categories_name", columnNames = "name"),
				@UniqueConstraint(name = "uk_categories_slug", columnNames = "slug")
		})
public class Category extends BaseEntity {

	@NotBlank
	@Size(max = 100)
	@Column(nullable = false, length = 100)
	private String name;

	@NotBlank
	@Size(max = 120)
	@Column(nullable = false, length = 120)
	private String slug;

	@Size(max = 1000)
	@Column(length = 1000)
	private String description;

	@Size(max = 2048)
	@Column(length = 2048)
	private String imageUrl;

	@Column(name = "is_active", nullable = false)
	private boolean isActive = true;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_id", foreignKey = @ForeignKey(name = "fk_categories_parent"))
	private Category parentCategory;

	@Column(name = "image_public_id", length = 255)
	private String imagePublicId;

	protected Category() {
	}

	public Category(String name, String slug, String description, String imageUrl, boolean isActive) {
		this.name = name;
		this.slug = slug;
		this.description = description;
		this.imageUrl = imageUrl;
		this.isActive = isActive;
	}

	public String getName() {
		return name;
	}

	public String getSlug() {
		return slug;
	}

	public String getDescription() {
		return description;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public boolean isActive() {
		return isActive;
	}

	public Category getParentCategory() {
		return parentCategory;
	}

	public String getImagePublicId() {
		return imagePublicId;
	}

	public void setImagePublicId(String imagePublicId) {
		this.imagePublicId = imagePublicId;
	}

	public void setParentCategory(Category parentCategory) {
		this.parentCategory = parentCategory;
	}

	public void update(String name, String slug, String description, String imageUrl, Boolean active) {
		update(name, slug, description, imageUrl, imagePublicId, parentCategory, active);
	}

	public void update(String name, String slug, String description, String imageUrl, String imagePublicId, Category parentCategory, Boolean active) {
		this.name = name;
		this.slug = slug;
		this.description = description;
		this.imageUrl = imageUrl;
		this.imagePublicId = imagePublicId;
		this.parentCategory = parentCategory;
		if (active != null) {
			this.isActive = active;
		}
	}

	public void deactivate() {
		this.isActive = false;
	}
}
