import * as THREE from 'three';

export const wallMaterial = new THREE.MeshStandardMaterial({
  color: '#F0EDE4',
  roughness: 0.85,
  metalness: 0.02,
});

export const floorMaterial = new THREE.MeshStandardMaterial({
  color: '#D8C9A8',
  roughness: 0.9,
  metalness: 0,
});

export const doorMaterial = new THREE.MeshStandardMaterial({
  color: '#C2692A',
  roughness: 0.6,
});

export const windowMaterial = new THREE.MeshStandardMaterial({
  color: '#185FA5',
  roughness: 0.1,
  metalness: 0.1,
  transparent: true,
  opacity: 0.45,
});

export const groundMaterial = new THREE.MeshStandardMaterial({
  color: '#EDEAE0',
  roughness: 1,
});
