export default function SunLight() {
  return (
    <>
      <ambientLight intensity={0.55} />
      <directionalLight
        position={[12, 18, 8]}
        intensity={1.4}
        castShadow
        shadow-mapSize-width={2048}
        shadow-mapSize-height={2048}
        shadow-camera-left={-20}
        shadow-camera-right={20}
        shadow-camera-top={20}
        shadow-camera-bottom={-20}
        shadow-camera-near={0.5}
        shadow-camera-far={60}
      />
      <hemisphereLight args={['#B8D4E8', '#C2692A', 0.35]} />
    </>
  );
}
