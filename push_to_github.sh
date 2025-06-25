#!/bin/bash

# Script to push AirportFinder project to GitHub
# This script avoids storing tokens in plain text

# GitHub username
GITHUB_USERNAME="KalyskyOM"
REPO_NAME="airportfinder"

# Check if git is already configured
if ! git config --get user.name > /dev/null; then
  echo "Setting up Git user name and email..."
  read -p "Enter your Git user name: " GIT_USER_NAME
  read -p "Enter your Git email: " GIT_USER_EMAIL
  
  git config --global user.name "$GIT_USER_NAME"
  git config --global user.email "$GIT_USER_EMAIL"
fi

# Check if repository exists on remote
echo "Checking for remote repository..."
if ! git remote -v | grep -q origin; then
  echo "Setting up remote repository..."
  echo "Please enter your GitHub Personal Access Token when prompted"
  echo "Note: The token will not be stored in this script for security reasons"
  
  # Add the remote using HTTPS
  git remote add origin "https://github.com/$GITHUB_USERNAME/$REPO_NAME.git"
  
  echo "Remote repository added."
else
  echo "Remote repository already configured."
fi

# Stage all files (excluding those in .gitignore)
echo "Staging files..."
git add .

# Commit changes if there are any
if git diff --staged --quiet; then
  echo "No changes to commit."
else
  echo "Committing changes..."
  read -p "Enter commit message: " COMMIT_MESSAGE
  git commit -m "${COMMIT_MESSAGE:-Update AirportFinder project}"
fi

# Push to GitHub
echo "Pushing to GitHub..."
echo "You may be prompted for your GitHub username and personal access token"
git push -u origin main || git push -u origin master

echo "Push completed!"
